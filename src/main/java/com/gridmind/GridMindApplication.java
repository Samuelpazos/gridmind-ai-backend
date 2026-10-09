package com.gridmind;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GridMindApplication {

    public static void main(String[] args) {
        SpringApplication.run(GridMindApplication.class, args);
    }
}
