package com.railway.train.member.controller;

import com.railway.train.common.resp.CommonResp;
import com.railway.train.member.req.MemberRegisterReq;
import com.railway.train.member.service.MemberService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/member")
public class MemberController {
    @Resource
    private MemberService memberService;

    @GetMapping("/count")
    public CommonResp<Integer> count() {
        int count = memberService.count();
        /*CommonResp<Integer> commonResp = new CommonResp<>();
        commonResp.setContent(count);
        return commonResp;*/
        return new CommonResp<>(count);
    }

    @PostMapping("/register")
    public CommonResp<Long> register(MemberRegisterReq req) {
        long register = memberService.register(req);
        /*CommonResp<Long> commonResp = new CommonResp<>();
        commonResp.setContent(register);
        return commonResp;*/
        return new CommonResp<>(register);
    }
}
