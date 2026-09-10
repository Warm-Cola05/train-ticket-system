package com.railway.train.business.config;


import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
@ComponentScan("com.railway")
@MapperScan("com.railway.train.business.mapper")
@EnableFeignClients("com.railway.train.business.feign")
@EnableCaching
public class BusinessApplication {

    private static final Logger LOG = LoggerFactory.getLogger(BusinessApplication.class);

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(BusinessApplication.class);
        Environment env = app.run(args).getEnvironment();
        LOG.info("启动成功！！");
        LOG.info("测试地址: \thttp://127.0.0.1:{}{}/hello", env.getProperty("server.port"), env.getProperty("server.servlet.context-path"));
//        //限流规则
//        initFlowRules();
//        LOG.info("限流规则初始化完成");
    }

    private static void initFlowRules(){
        List<FlowRule> rules = new ArrayList<>();
        FlowRule rule = new FlowRule();
        // 资源名称：要保护的接口/资源名 HelloWorld
        rule.setResource("doConfirm");
        // 限流模式：QPS限流（不是线程数限流）
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        // 设置阈值：QPS=20，每秒最多允许20次请求
        rule.setCount(20);
        rules.add(rule);
        // 加载规则到Sentinel
        FlowRuleManager.loadRules(rules);
    }

}
