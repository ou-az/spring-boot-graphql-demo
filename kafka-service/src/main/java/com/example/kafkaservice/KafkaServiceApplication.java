package com.example.kafkaservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class KafkaServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(KafkaServiceApplication.class, args);
    }
}
