package com.ai.companion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CompanionApplication {

	public static void main(String[] args) {
		SpringApplication.run(CompanionApplication.class, args);
	}

}
