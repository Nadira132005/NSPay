package com.nspay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NSPayApplication {

	public static void main(String[] args) {
		SpringApplication.run(NSPayApplication.class, args);
	}
}
