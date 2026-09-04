//package com.railway.train.batch.job;
//
//import org.springframework.scheduling.annotation.EnableScheduling;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
////只适合单体应用，不适合集群，没法实时更改定时任务状态和策略，适合小项目，快速实现定时任务
//@EnableScheduling
//@Component
//public class SpringBootTestJob {
//
//    @Scheduled(cron = "0/5 * * * * ?")
//    private void test(){
//        System.out.println("SpringBootTest");
//    }
//}
