package com.railway.train.member.config;


import com.railway.train.common.interceptor.LogInterceptor;
import com.railway.train.common.interceptor.MemberInterceptor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SpringMvcConfig implements WebMvcConfigurer {
    @Resource
    LogInterceptor logInterceptor;
   @Resource
   MemberInterceptor memberInterceptor;


   @Override
   public void addInterceptors(InterceptorRegistry registry) {
       registry.addInterceptor(logInterceptor);

       registry.addInterceptor(memberInterceptor)

               .addPathPatterns("/**")
               .excludePathPatterns(
                       "/member/hello",
                       "/member/send-code",
                       "/member/login"
               );
   }
}
