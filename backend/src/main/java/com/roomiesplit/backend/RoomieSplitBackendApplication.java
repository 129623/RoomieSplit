package com.roomiesplit.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.mybatis.spring.annotation.MapperScan("com.roomiesplit.backend.mapper")
public class RoomieSplitBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoomieSplitBackendApplication.class, args);
    }

}
