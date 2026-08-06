package com.typenglish;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.typenglish.mapper")
public class TypEnglishApplication {
    public static void main(String[] args) {
        SpringApplication.run(TypEnglishApplication.class, args);
    }
}
