package com.railway.train.business.service;

import cn.hutool.core.date.DateTime;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.railway.train.business.domain.ConfirmOrder;
import com.railway.train.business.enums.ConfirmOrderStatusEnum;
import com.railway.train.business.enums.RocketMQTopicEnum;
import com.railway.train.business.mapper.ConfirmOrderMapper;
import com.railway.train.business.req.ConfirmOrderDoReq;
import com.railway.train.common.context.LoginMemberContext;
import com.railway.train.common.exception.BusinessException;
import com.railway.train.common.exception.BusinessExceptionEnum;
import com.railway.train.common.util.SnowUtil;
import jakarta.annotation.Resource;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

/**
 * 购票前置服务：只做校验和保存订单，随后发 MQ 消息异步出票，
 * 这样可以立刻给用户反馈（订单已受理），不用等完整的选座购票流程走完。
 */
@Service
public class BeforeConfirmOrderService {

    private static final Logger LOG = LoggerFactory.getLogger(BeforeConfirmOrderService.class);

    @Resource
    private ConfirmOrderMapper confirmOrderMapper;

    @Resource
    private SkTokenService skTokenService;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    /**
     * 校验令牌余量 -> 保存确认订单（初始状态） -> 发送 MQ 消息
     *
     * @return 确认订单ID，作为用户后续查询排队/结果的凭证
     */
    @SentinelResource(value = "beforeDoConfirm", blockHandler = "beforeDoConfirmBlock")
    public Long beforeDoConfirm(ConfirmOrderDoReq req) {
        Long id = null;
        // 根据前端传入的 lineNumber 额外插入若干条订单，用于体验排队效果。
        // lineNumber=5 时共插入 6 条（1 条真实 + 5 条模拟），最后返回的是最后一条的ID，
        // 这样自己的订单排在队尾，能直观看到排队人数一点点减少。生产环境前端传 0。
        for (int i = 0; i < req.getLineNumber() + 1; i++) {
            // 校验令牌余量
            boolean validSkToken = skTokenService.validSkToken(req.getDate(), req.getTrainCode(), LoginMemberContext.getId());
            if (validSkToken) {
                LOG.info("令牌校验通过");
            } else {
                LOG.info("令牌校验不通过");
                throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_SK_TOKEN_FAIL);
            }

            // 保存确认订单，状态为初始，真正的出票由消费者完成
            DateTime now = DateTime.now();
            ConfirmOrder confirmOrder = new ConfirmOrder();
            confirmOrder.setId(SnowUtil.getSnowflakeNextId());
            confirmOrder.setCreateTime(now);
            confirmOrder.setUpdateTime(now);
            confirmOrder.setMemberId(req.getMemberId());
            confirmOrder.setDate(req.getDate());
            confirmOrder.setTrainCode(req.getTrainCode());
            confirmOrder.setStart(req.getStart());
            confirmOrder.setEnd(req.getEnd());
            confirmOrder.setDailyTrainTicketId(req.getDailyTrainTicketId());
            confirmOrder.setStatus(ConfirmOrderStatusEnum.INIT.getCode());
            confirmOrder.setTickets(JSON.toJSONString(req.getTickets()));
            confirmOrderMapper.insert(confirmOrder);

            // 发送 MQ 消息，排队出票。destination 格式：topic:tag
            // 同转异：把日志流水号塞进消息体，消费者侧恢复 MDC，才能把两段日志串起来
            req.setLogId(MDC.get("LOG_ID"));
            String reqJson = JSON.toJSONString(req);
            LOG.info("排队购票，发送mq开始，消息内容：{}", reqJson);
            rocketMQTemplate.convertAndSend(RocketMQTopicEnum.CONFIRM_ORDER.getCode() + ":do", reqJson);
            LOG.info("排队购票，发送mq结束");

            id = confirmOrder.getId();
        }
        return id;
    }

    /**
     * 降级方法，需包含限流方法的所有参数和 BlockException 参数
     */
    public Long beforeDoConfirmBlock(ConfirmOrderDoReq req, BlockException e) {
        LOG.info("购票请求被限流：{}", req);
        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_FLOW_EXCEPTION);
    }
}
