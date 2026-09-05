package com.railway.train.business.controller.admin;

import com.railway.train.common.context.LoginMemberContext;
import com.railway.train.common.resp.CommonResp;
import com.railway.train.common.resp.PageResp;
import com.railway.train.business.req.DailyTrainTicketQueryReq;
import com.railway.train.business.req.DailyTrainTicketSaveReq;
import com.railway.train.business.resp.DailyTrainTicketQueryResp;
import com.railway.train.business.service.DailyTrainTicketService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/daily-train-ticket")
public class DailyTrainTicketAdminController {
    @Resource
    private DailyTrainTicketService dailyTrainTicketService;

    @PostMapping("/save")
    public CommonResp<Object> save(@Valid @RequestBody DailyTrainTicketSaveReq req) {
        dailyTrainTicketService.save(req);
        return new CommonResp<>();
    }

    @GetMapping("/query-list")
    public CommonResp<PageResp<DailyTrainTicketQueryResp>> queryList(@Valid DailyTrainTicketQueryReq req) {
        req.setMemberId(LoginMemberContext.getId());
        PageResp<DailyTrainTicketQueryResp> list = dailyTrainTicketService.queryList(req);
        return new CommonResp<>(list);
    }

    @DeleteMapping("/delete/{id}")
    public CommonResp<Object> delete(@PathVariable Long id) {
        dailyTrainTicketService.delete(id);
        return new CommonResp<>();
    }
}
