package com.railway.train.business.controller;

import com.railway.train.business.req.ConfirmOrderDoReq;
import com.railway.train.business.req.ConfirmOrderQueryReq;
import com.railway.train.business.resp.ConfirmOrderQueryResp;
import com.railway.train.business.service.ConfirmOrderService;
import com.railway.train.common.context.LoginMemberContext;
import com.railway.train.common.resp.CommonResp;
import com.railway.train.common.resp.PageResp;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/confirm-order")
public class ConfirmOrderController {
    @Resource
    private ConfirmOrderService confirmOrderService;

    @PostMapping("/do")
    public CommonResp<Object> doConfirm(@Valid @RequestBody ConfirmOrderDoReq req) throws Exception {
        req.setMemberId(LoginMemberContext.getId());
        confirmOrderService.doConfirm(req);
        return new CommonResp<>();
    }

}
