package com.railway.train.business.controller;

import com.railway.train.business.req.ConfirmOrderDoReq;
import com.railway.train.business.service.BeforeConfirmOrderService;
import com.railway.train.business.service.ConfirmOrderService;
import com.railway.train.common.context.LoginMemberContext;
import com.railway.train.common.resp.CommonResp;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/confirm-order")
public class ConfirmOrderController {

    private static final Logger LOG = LoggerFactory.getLogger(ConfirmOrderController.class);

    @Resource
    private BeforeConfirmOrderService beforeConfirmOrderService;

    @Resource
    private ConfirmOrderService confirmOrderService;

    /**
     * 下单：只做前置校验 + 保存订单 + 发MQ消息，立刻返回订单ID给用户，
     * 真正的出票由消费者异步完成，用户凭此ID轮询排队/结果。
     */
    @PostMapping("/do")
    public CommonResp<Object> doConfirm(@Valid @RequestBody ConfirmOrderDoReq req) {
        req.setMemberId(LoginMemberContext.getId());
        Long id = beforeConfirmOrderService.beforeDoConfirm(req);
        LOG.info("下单受理成功，确认订单ID：{}", id);
        // Long 转 String 返回，避免前端 JS 数字精度丢失
        return new CommonResp<>(String.valueOf(id));
    }

    /**
     * 查询排队数量
     * 返回值：0 处理中；-1 成功；-2 失败；-3 无票；-4 取消；正数表示前面还有几人
     */
    @GetMapping("/query-line-count/{id}")
    public CommonResp<Integer> queryLineCount(@PathVariable Long id) {
        Integer count = confirmOrderService.queryLineCount(id);
        return new CommonResp<>(count);
    }

    /**
     * 取消订单
     */
    @GetMapping("/cancel/{id}")
    public CommonResp<Object> cancel(@PathVariable Long id) {
        confirmOrderService.cancel(id);
        return new CommonResp<>();
    }
}
