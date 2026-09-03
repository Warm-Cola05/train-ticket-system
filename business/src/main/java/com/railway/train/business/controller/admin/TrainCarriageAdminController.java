package com.railway.train.business.controller.admin;

import com.railway.train.common.context.LoginMemberContext;
import com.railway.train.common.resp.CommonResp;
import com.railway.train.common.resp.PageResp;
import com.railway.train.business.req.TrainCarriageQueryReq;
import com.railway.train.business.req.TrainCarriageSaveReq;
import com.railway.train.business.resp.TrainCarriageQueryResp;
import com.railway.train.business.service.TrainCarriageService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/train-carriage")
public class TrainCarriageAdminController {
    @Resource
    private TrainCarriageService trainCarriageService;

    @PostMapping("/save")
    public CommonResp<Object> save(@Valid @RequestBody TrainCarriageSaveReq req) {
        trainCarriageService.save(req);
        return new CommonResp<>();
    }

    @GetMapping("/query-list")
    public CommonResp<PageResp<TrainCarriageQueryResp>> queryList(@Valid TrainCarriageQueryReq req) {
        req.setMemberId(LoginMemberContext.getId());
        PageResp<TrainCarriageQueryResp> list = trainCarriageService.queryList(req);
        return new CommonResp<>(list);
    }

    @DeleteMapping("/delete/{id}")
    public CommonResp<Object> delete(@PathVariable Long id) {
        trainCarriageService.delete(id);
        return new CommonResp<>();
    }
}
