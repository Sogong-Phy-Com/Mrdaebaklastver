package com.mrdabak.dinnerservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EntityScan(basePackages = "com.mrdabak.dinnerservice.model")
@EnableScheduling
public class DinnerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DinnerServiceApplication.class, args);
    }
}




