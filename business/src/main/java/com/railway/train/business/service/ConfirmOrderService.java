package com.railway.train.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.railway.train.business.domain.*;
import com.railway.train.business.enums.ConfirmOrderStatusEnum;
import com.railway.train.business.enums.RedisKeyPreEnum;
import com.railway.train.business.enums.SeatColEnum;
import com.railway.train.business.enums.SeatTypeEnum;
import com.railway.train.business.mapper.ConfirmOrderMapper;
import com.railway.train.business.req.ConfirmOrderDoReq;
import com.railway.train.business.req.ConfirmOrderQueryReq;
import com.railway.train.business.req.ConfirmOrderTicketReq;
import com.railway.train.business.resp.ConfirmOrderQueryResp;
import com.railway.train.common.exception.BusinessException;
import com.railway.train.common.exception.BusinessExceptionEnum;
import com.railway.train.common.resp.PageResp;
import com.railway.train.common.util.SnowUtil;
import jakarta.annotation.Resource;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ConfirmOrderService {

    private static final Logger LOG = LoggerFactory.getLogger(ConfirmOrderService.class);

    @Resource
    private ConfirmOrderMapper confirmOrderMapper;

    @Resource
    private DailyTrainTicketService dailyTrainTicketService;

    @Resource
    private DailyTrainCarriageService dailyTrainCarriageService;

    @Resource
    private DailyTrainSeatService dailyTrainSeatService;

    @Autowired
    private AfterConfirmOrderService afterConfirmOrderService;

    @Autowired
    private RedissonClient redissonClient;

    public void save(ConfirmOrderDoReq req) {
        DateTime now = DateTime.now();
        ConfirmOrder confirmOrder = BeanUtil.copyProperties(req, ConfirmOrder.class);
        if (ObjectUtil.isNull(confirmOrder.getId())) {
            confirmOrder.setId(SnowUtil.getSnowflakeNextId());
            confirmOrder.setCreateTime(now);
            confirmOrder.setUpdateTime(now);
            confirmOrderMapper.insert(confirmOrder);
        } else {
            confirmOrder.setUpdateTime(now);
            confirmOrderMapper.updateByPrimaryKey(confirmOrder);
        }
    }

    public PageResp<ConfirmOrderQueryResp> queryList(ConfirmOrderQueryReq req) {
        ConfirmOrderExample confirmOrderExample = new ConfirmOrderExample();
        confirmOrderExample.setOrderByClause("id desc");
        confirmOrderExample.createCriteria();

        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<ConfirmOrder> confirmOrderList = confirmOrderMapper.selectByExample(confirmOrderExample);

        PageInfo<ConfirmOrder> pageInfo = new PageInfo<>(confirmOrderList);
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());

        List<ConfirmOrderQueryResp> list = BeanUtil.copyToList(confirmOrderList, ConfirmOrderQueryResp.class);

        PageResp<ConfirmOrderQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(pageInfo.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    public void delete(Long id) {
        confirmOrderMapper.deleteByPrimaryKey(id);
    }

    /**
     * 出票（由 MQ 消费者触发）：
     * 按车次加分布式锁，抢到锁后循环处理该车次所有待出票订单（厨师模型：同一车次的订单按顺序处理）。
     * 这样拿到令牌的请求最终都会被处理，不会因为抢不到锁而丢失购票资格。
     */
    public void sell(ConfirmOrderDoReq req) {
        // 同转异：把 HTTP 线程的日志流水号恢复到消费线程，这样一次购票的两段日志能用同一个 logId 串起来
        if (StrUtil.isNotBlank(req.getLogId())) {
            MDC.put("LOG_ID", req.getLogId());
        }
        String lockKey = RedisKeyPreEnum.CONFIRM_ORDER + "-"
                + DateUtil.formatDate(req.getDate()) + "-" + req.getTrainCode();
        RLock lock = null;
        try {
            lock = redissonClient.getLock(lockKey);
            // 使用 redisson，自带看门狗
            boolean tryLock = lock.tryLock(0, TimeUnit.SECONDS);
            if (tryLock) {
                LOG.info("恭喜，抢到锁了！");
            } else {
                // 没抢到锁说明已有线程在处理本车次的订单，本条消息直接结束即可
                LOG.info("很遗憾，没抢到锁，本车次已有线程在处理，本条消息不处理");
                return;
            }

            // 循环出票：一次把该车次所有待出票订单处理完
            while (true) {
                ConfirmOrderExample example = new ConfirmOrderExample();
                example.setOrderByClause("create_time asc");
                example.or().andDateEqualTo(req.getDate())
                        .andTrainCodeEqualTo(req.getTrainCode())
                        .andStatusEqualTo(ConfirmOrderStatusEnum.INIT.getCode());
                List<ConfirmOrder> confirmOrders = confirmOrderMapper.selectByExample(example);
                if (CollUtil.isEmpty(confirmOrders)) {
                    LOG.info("本车次没有待出票的订单，出票结束");
                    break;
                }

                ConfirmOrder confirmOrder = confirmOrders.get(0);
                LOG.info("开始处理订单：{}", confirmOrder.getId());
                // 开始出票前先置为处理中，避免被重复处理
                updateStatus(confirmOrder.getId(), ConfirmOrderStatusEnum.PENDING.getCode());
                try {
                    // 模拟出票耗时，方便观察排队效果
                    Thread.sleep(200);
                    doConfirm(confirmOrder);
                    updateStatus(confirmOrder.getId(), ConfirmOrderStatusEnum.SUCCESS.getCode());
                    LOG.info("订单{}出票成功", confirmOrder.getId());
                } catch (BusinessException e) {
                    // 余票不足等已知业务异常，标记为无票
                    LOG.error("订单{}出票失败（业务异常）：{}", confirmOrder.getId(), e.getMessage());
                    updateStatus(confirmOrder.getId(), ConfirmOrderStatusEnum.EMPTY.getCode());
                } catch (Exception e) {
                    LOG.error("订单{}出票异常", confirmOrder.getId(), e);
                    updateStatus(confirmOrder.getId(), ConfirmOrderStatusEnum.FAILURE.getCode());
                }
            }
        } catch (InterruptedException e) {
            LOG.error("出票被中断", e);
        } finally {
            LOG.info("出票结束，释放锁");
            if (null != lock && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
            // 消费线程是线程池复用的，必须清理 MDC，防止流水号串到下一次消费
            MDC.remove("LOG_ID");
        }
    }

    /**
     * 对单条确认订单执行真正的选座购票逻辑
     */
    private void doConfirm(ConfirmOrder confirmOrder) throws Exception {
        Date date = confirmOrder.getDate();
        String trainCode = confirmOrder.getTrainCode();
        String start = confirmOrder.getStart();
        String end = confirmOrder.getEnd();
        List<ConfirmOrderTicketReq> tickets = JSON.parseArray(confirmOrder.getTickets(), ConfirmOrderTicketReq.class);

        DailyTrainTicket dailyTrainTicket = dailyTrainTicketService.selectByUnique(date, trainCode, start, end);
        LOG.info("查询余票记录：{}", dailyTrainTicket);
        // 预扣减余票数量，并判断余票是否足够
        reduceTickets(tickets, dailyTrainTicket);

        // 最终的选座结果
        List<DailyTrainSeat> finalSeatList = new ArrayList<>();
        // 计算相对第一个座位的偏移值
        // 比如选择的是C1,D2，则偏移值是：[0,5]
        // 比如选择的是A1,B1,C1，则偏移值是：[0,1,2]
        ConfirmOrderTicketReq ticketReq0 = tickets.get(0);
        if (StrUtil.isNotBlank(ticketReq0.getSeat())) {
            LOG.info("本次购票有选座");
            List<SeatColEnum> colEnumList = SeatColEnum.getColsByType(ticketReq0.getSeatTypeCode());
            LOG.info("本次选座的座位类型包含的列：{}", colEnumList);
            // 组成和前端两排选座一样的列表，用于作参照的座位列表，例：referSeatList = {A1, C1, D1, F1, A2, C2, D2, F2}
            List<String> referSeatList = new ArrayList<>();
            for (int i = 1; i <= 2; i++) {
                for (SeatColEnum seatColEnum : colEnumList) {
                    referSeatList.add(seatColEnum.getCode() + i);
                }
            }
            LOG.info("用于作参照的两排座位：{}", referSeatList);

            List<Integer> offsetList = new ArrayList<>();
            // 绝对偏移值，即：在参照座位列表中的位置
            List<Integer> aboluteOffsetList = new ArrayList<>();
            for (ConfirmOrderTicketReq ticketReq : tickets) {
                int index = referSeatList.indexOf(ticketReq.getSeat());
                aboluteOffsetList.add(index);
            }
            LOG.info("计算得到所有座位的绝对偏移值：{}", aboluteOffsetList);
            for (Integer index : aboluteOffsetList) {
                int offset = index - aboluteOffsetList.get(0);
                offsetList.add(offset);
            }
            LOG.info("计算得到所有座位的相对第一个座位的偏移值：{}", offsetList);

            getSeat(finalSeatList,
                    date,
                    trainCode,
                    ticketReq0.getSeatTypeCode(),
                    ticketReq0.getSeat().split("")[0], // 从A1得到A
                    offsetList,
                    dailyTrainTicket.getStartIndex(),
                    dailyTrainTicket.getEndIndex()
            );
        } else {
            LOG.info("本次购票无选座");
            for (ConfirmOrderTicketReq ticketReq : tickets) {
                getSeat(finalSeatList,
                        date,
                        trainCode,
                        ticketReq.getSeatTypeCode(),
                        null,
                        null,
                        dailyTrainTicket.getStartIndex(),
                        dailyTrainTicket.getEndIndex()
                );
            }
        }
        LOG.info("最终选中的座位：{}", finalSeatList);

        // 选不到座位时，按无票处理
        if (CollUtil.isEmpty(finalSeatList)) {
            LOG.info("本次购票未选到合适的座位");
            throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR);
        }

        try {
            afterConfirmOrderService.afterDoConfirm(dailyTrainTicket, finalSeatList, tickets, confirmOrder);
        } catch (Exception e) {
            LOG.error("保存购票信息异常", e);
            throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_EXCEPTION);
        }
    }

    /**
     * 更新订单状态
     */
    private void updateStatus(Long id, String status) {
        ConfirmOrder confirmOrder = new ConfirmOrder();
        confirmOrder.setId(id);
        confirmOrder.setUpdateTime(DateTime.now());
        confirmOrder.setStatus(status);
        confirmOrderMapper.updateByPrimaryKeySelective(confirmOrder);
    }

    /**
     * 查询排队数量：
     * 正数表示前面还有几个人；负数表示终态（-1成功，-2失败，-3无票，-4取消）；0表示正在处理中
     */
    public Integer queryLineCount(Long id) {
        ConfirmOrder confirmOrder = confirmOrderMapper.selectByPrimaryKey(id);
        if (ObjectUtil.isNull(confirmOrder)) {
            throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_EXCEPTION);
        }
        ConfirmOrderStatusEnum statusEnum = EnumUtil.getBy(ConfirmOrderStatusEnum::getCode, confirmOrder.getStatus());
        int result = switch (statusEnum) {
            case PENDING -> 0;      // 处理中
            case SUCCESS -> -1;     // 成功
            case FAILURE -> -2;     // 失败
            case EMPTY -> -3;       // 无票
            case CANCEL -> -4;      // 取消
            default -> 999;         // 初始状态，需要二次查询具体排队数量
        };
        if (result != 999) {
            return result;
        }

        // 排在第几位：统计同车次、同日期、创建时间早于本订单，且状态为初始或处理中的订单数
        // 等价于 where (date=? and train_code=? and create_time<? and status='I')
        //        or (date=? and train_code=? and create_time<? and status='P')
        ConfirmOrderExample confirmOrderExample = new ConfirmOrderExample();
        confirmOrderExample.or().andDateEqualTo(confirmOrder.getDate())
                .andTrainCodeEqualTo(confirmOrder.getTrainCode())
                .andCreateTimeLessThan(confirmOrder.getCreateTime())
                .andStatusEqualTo(ConfirmOrderStatusEnum.INIT.getCode());
        confirmOrderExample.or().andDateEqualTo(confirmOrder.getDate())
                .andTrainCodeEqualTo(confirmOrder.getTrainCode())
                .andCreateTimeLessThan(confirmOrder.getCreateTime())
                .andStatusEqualTo(ConfirmOrderStatusEnum.PENDING.getCode());
        long lineCount = confirmOrderMapper.countByExample(confirmOrderExample);
        LOG.info("订单{}前面还有{}人排队", id, lineCount);
        return (int) lineCount;
    }

    /**
     * 取消订单
     */
    public void cancel(Long id) {
        LOG.info("取消订单：{}", id);
        updateStatus(id, ConfirmOrderStatusEnum.CANCEL.getCode());
    }

    private void getSeat(List<DailyTrainSeat> finalSeatList, Date date, String trainCode, String seatType, String column, List<Integer> offsetList, Integer startIndex, Integer endIndex) {
        List<DailyTrainSeat> getSeatList = new ArrayList<>();
        List<DailyTrainCarriage> carriageList = dailyTrainCarriageService.selectBySeatType(date, trainCode, seatType);
        LOG.info("共查出{}个符合条件的车厢", carriageList.size());

        // 一个车箱一个车箱的获取座位数据
        for (DailyTrainCarriage dailyTrainCarriage : carriageList) {
            LOG.info("开始从车厢{}选座", dailyTrainCarriage.getIndex());
            getSeatList = new ArrayList<>();
            List<DailyTrainSeat> seatList = dailyTrainSeatService.selectByCarriage(date, trainCode, dailyTrainCarriage.getIndex());
            LOG.info("车厢{}的座位数：{}", dailyTrainCarriage.getIndex(), seatList.size());
            for (int i = 0; i < seatList.size(); i++) {
                DailyTrainSeat dailyTrainSeat = seatList.get(i);
                Integer seatIndex = dailyTrainSeat.getCarriageSeatIndex();
                String col = dailyTrainSeat.getCol();

                // 判断当前座位不能被选中过
                boolean alreadyChooseFlag = false;
                for (DailyTrainSeat finalSeat : finalSeatList) {
                    if (finalSeat.getId().equals(dailyTrainSeat.getId())) {
                        alreadyChooseFlag = true;
                        break;
                    }
                }
                if (alreadyChooseFlag) {
                    LOG.info("座位{}被选中过，不能重复选中，继续判断下一个座位", seatIndex);
                    continue;
                }

                // 判断column，有值的话要比对列号
                if (StrUtil.isBlank(column)) {
                    LOG.info("无选座");
                } else {
                    if (!column.equals(col)) {
                        LOG.info("座位{}列值不对，继续判断下一个座位，当前列值：{}，目标列值：{}", seatIndex, col, column);
                        continue;
                    }
                }

                boolean isChoose = calSell(dailyTrainSeat, startIndex, endIndex);
                if (isChoose) {
                    LOG.info("选中座位");
                    getSeatList.add(dailyTrainSeat);
                } else {
                    continue;
                }

                // 根据offset选剩下的座位
                boolean isGetAllOffsetSeat = true;
                if (CollUtil.isNotEmpty(offsetList)) {
                    LOG.info("有偏移值：{}，校验偏移的座位是否可选", offsetList);
                    // 从索引1开始，索引0就是当前已选中的票
                    for (int j = 1; j < offsetList.size(); j++) {
                        Integer offset = offsetList.get(j);
                        // 座位在库的索引是从1开始
                        int nextIndex = i + offset;

                        // 有选座时，一定是在同一个车箱
                        if (nextIndex >= seatList.size()) {
                            LOG.info("座位{}不可选，偏移后的索引超出了这个车箱的座位数", nextIndex);
                            isGetAllOffsetSeat = false;
                            break;
                        }

                        DailyTrainSeat nextDailyTrainSeat = seatList.get(nextIndex);
                        boolean isChooseNext = calSell(nextDailyTrainSeat, startIndex, endIndex);
                        if (isChooseNext) {
                            LOG.info("座位{}被选中", nextDailyTrainSeat.getCarriageSeatIndex());
                            getSeatList.add(nextDailyTrainSeat);
                        } else {
                            LOG.info("座位{}不可选", nextDailyTrainSeat.getCarriageSeatIndex());
                            isGetAllOffsetSeat = false;
                            break;
                        }
                    }
                }
                if (!isGetAllOffsetSeat) {
                    getSeatList = new ArrayList<>();
                    continue;
                }

                // 保存选好的座位
                finalSeatList.addAll(getSeatList);
                return;
            }
        }
    }

    /**
     * 计算某座位在区间内是否可卖
     * 例：sell=10001，本次购买区间站1~4，则区间已售000
     * 全部是0，表示这个区间可买；只要有1，就表示区间内已售过票
     *
     * 选中后，要计算购票后的sell，比如原来是10001，本次购买区间站1~4
     * 方案：构造本次购票造成的售卖信息01110，和原sell 10001按位与，最终得到11111
     */
    private boolean calSell(DailyTrainSeat dailyTrainSeat, Integer startIndex, Integer endIndex) {
        // 00001, 00000
        String sell = dailyTrainSeat.getSell();
        //  000, 000
        String sellPart = sell.substring(startIndex, endIndex);
        if (Integer.parseInt(sellPart) > 0) {
            LOG.info("座位{}在本次车站区间{}~{}已售过票，不可选中该座位", dailyTrainSeat.getCarriageSeatIndex(), startIndex, endIndex);
            return false;
        } else {
            LOG.info("座位{}在本次车站区间{}~{}未售过票，可选中该座位", dailyTrainSeat.getCarriageSeatIndex(), startIndex, endIndex);
            //  111,   111
            String curSell = sellPart.replace('0', '1');
            // 0111,  0111
            curSell = StrUtil.fillBefore(curSell, '0', endIndex);
            // 01110, 01110
            curSell = StrUtil.fillAfter(curSell, '0', sell.length());

            // 当前区间售票信息curSell 01110与库里的已售信息sell 00001按位与，即可得到该座位卖出此票后的售票详情
            // 15(01111), 14(01110 = 01110|00000)
            int newSellInt = NumberUtil.binaryToInt(curSell) | NumberUtil.binaryToInt(sell);
            //  1111,  1110
            String newSell = NumberUtil.getBinaryStr(newSellInt);
            // 01111, 01110
            newSell = StrUtil.fillBefore(newSell, '0', sell.length());
            LOG.info("座位{}被选中，原售票信息：{}，车站区间：{}~{}，即：{}，最终售票信息：{}"
                    , dailyTrainSeat.getCarriageSeatIndex(), sell, startIndex, endIndex, curSell, newSell);
            dailyTrainSeat.setSell(newSell);
            return true;
        }
    }

    private static void reduceTickets(List<ConfirmOrderTicketReq> tickets, DailyTrainTicket dailyTrainTicket) {
        for (ConfirmOrderTicketReq ticketReq : tickets) {
            String seatTypeCode = ticketReq.getSeatTypeCode();
            SeatTypeEnum seatTypeEnum = EnumUtil.getBy(SeatTypeEnum::getCode, seatTypeCode);
            switch (seatTypeEnum) {
                case YDZ -> {
                    int countLeft = dailyTrainTicket.getYdz() - 1;
                    if (countLeft < 0) {
                        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    }
                    dailyTrainTicket.setYdz(countLeft);
                }
                case EDZ -> {
                    int countLeft = dailyTrainTicket.getEdz() - 1;
                    if (countLeft < 0) {
                        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    }
                    dailyTrainTicket.setEdz(countLeft);
                }
                case RW -> {
                    int countLeft = dailyTrainTicket.getRw() - 1;
                    if (countLeft < 0) {
                        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    }
                    dailyTrainTicket.setRw(countLeft);
                }
                case YW -> {
                    int countLeft = dailyTrainTicket.getYw() - 1;
                    if (countLeft < 0) {
                        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    }
                    dailyTrainTicket.setYw(countLeft);
                }
            }
        }
    }
}
