package com.example.naim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.example.naim")
public class IntelliStudyApplication {
    public static void main(String[] args) {
        SpringApplication.run(IntelliStudyApplication.class, args);
    }
}
