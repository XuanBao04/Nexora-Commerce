package com.nexoracommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NexoraCommerceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexoraCommerceApplication.class, args);
    }
}
