package com.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan("com.ai.mapper")
@EnableAsync
public class AiCustomerServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(AiCustomerServiceApplication.class, args);
	}
}