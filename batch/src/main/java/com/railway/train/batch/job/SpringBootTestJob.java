package com.railway.train.batch.job;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@EnableScheduling
@Component

public class SpringBootTestJob {

    @Scheduled(cron = "0/5 * * * * ?")
    private void test(){
        System.out.println("SpringBootTest");
    }
}
