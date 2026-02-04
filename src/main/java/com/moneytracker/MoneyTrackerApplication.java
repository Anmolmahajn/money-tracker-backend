package com.moneytracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // ADD THIS
public class MoneyTrackerApplication {
    public static void main(String[] args) {
        SpringApplication.run(MoneyTrackerApplication.class, args);
    }
}
