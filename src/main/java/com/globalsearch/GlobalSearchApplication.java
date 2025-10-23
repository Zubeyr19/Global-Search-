package com.globalsearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GlobalSearchApplication {
    public static void main(String[] args) {
        SpringApplication.run(GlobalSearchApplication.class, args);
        System.out.println("=================================");
        System.out.println("Global Search Application Started!");
        System.out.println("Visit: http://localhost:8080");
        System.out.println("=================================");
    }
}