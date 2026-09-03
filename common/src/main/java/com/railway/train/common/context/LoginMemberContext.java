package com.railway.train.common.context;

import com.railway.train.common.resp.MemberLoginResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginMemberContext {
    private static final Logger LOG = LoggerFactory.getLogger(LoginMemberContext.class);

    private static ThreadLocal<MemberLoginResp> member = new ThreadLocal<>();

    public static MemberLoginResp getMember() {
        return member.get();
    }

    public static void setMember(MemberLoginResp member) {
        LoginMemberContext.member.set(member);
    }

    public static Long getId() {
        try {
            // 没经过 MemberInterceptor（比如 admin/未登录接口）时 ThreadLocal 是空的，
            // 直接返回 null，不要抛 NPE；下游未使用 memberId 即可正常工作。
            MemberLoginResp m = member.get();
            return m == null ? null : m.getId();
        } catch (Exception e) {
            LOG.error("获取登录会员信息异常", e);
            return null;
        }
    }

}
