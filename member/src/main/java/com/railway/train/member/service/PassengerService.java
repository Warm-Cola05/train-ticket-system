package com.railway.train.member.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.railway.train.common.context.LoginMemberContext;
import com.railway.train.common.util.SnowUtil;
import com.railway.train.member.domain.Passenger;
import com.railway.train.member.domain.PassengerExample;
import com.railway.train.member.mapper.PassengerMapper;
import com.railway.train.member.req.PassengerQueryReq;
import com.railway.train.member.req.PassengerSaveReq;
import com.railway.train.member.resp.PageResp;
import com.railway.train.member.resp.PassengerQueryResp;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PassengerService {
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(PassengerService.class);
    @Resource
    private PassengerMapper passengerMapper;
    public void save(PassengerSaveReq req){
        DateTime now = DateTime.now();
        Passenger passenger = BeanUtil.copyProperties(req, Passenger.class);
        if (ObjectUtil.isNull(passenger.getMemberId())) {
            passenger.setMemberId(LoginMemberContext.getId());
            passenger.setId(SnowUtil.getSnowflakeNextId());
            passenger.setCreateTime(now);
            passenger.setUpdateTime(now);
            passengerMapper.insert(passenger);
        } else {
            passenger.setUpdateTime(now);
            passengerMapper.updateByPrimaryKey(passenger);
        }
    }

    public PageResp<PassengerQueryResp> queryList(PassengerQueryReq req){
        PassengerExample passengerExample = new PassengerExample();
        PassengerExample.Criteria criteria = passengerExample.createCriteria();
        if (ObjectUtil.isNotNull(req.getMemberId())){
            criteria.andMemberIdEqualTo(req.getMemberId());
        }
        LOG.info("查询页码：{}",req.getPage());
        LOG.info("查询条数：{}",req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<Passenger> passengerList = passengerMapper.selectByExample(passengerExample);
        PageInfo<Passenger> pageInfo = new PageInfo<>(passengerList);
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());
        List<PassengerQueryResp> list = BeanUtil.copyToList(passengerList, PassengerQueryResp.class);
        PageResp<PassengerQueryResp> pageResp = new PageResp<>();
        pageResp.setList(list);
        pageResp.setTotal(pageInfo.getTotal());
        return pageResp;
    }

    public void delete(Long id){
        passengerMapper.deleteByPrimaryKey(id);
    }
}
