package com.railway.train.member.controller.admin;


import com.railway.train.common.resp.CommonResp;
import com.railway.train.common.resp.PageResp;
import com.railway.train.member.req.TicketQueryReq;
import com.railway.train.member.resp.TicketQueryResp;
import com.railway.train.member.service.TicketService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/ticket")
public class TicketAdminController {

    @Resource
    private TicketService ticketService;

    @GetMapping("/query-list")
    public CommonResp<PageResp<TicketQueryResp>> queryList(@Valid TicketQueryReq req) {
        PageResp<TicketQueryResp> list = ticketService.queryList(req);
        return new CommonResp<>(list);
    }

}
