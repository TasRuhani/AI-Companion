package com.ai.companion;

import com.ai.companion.service.OllamaService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CompanionApplication {

	public static void main(String[] args) {
		SpringApplication.run(CompanionApplication.class, args);
	}

//	@Bean
//	CommandLineRunner test(OllamaService ollamaService) {
//		return args -> {
//			String response = ollamaService.generate(
//					"what is the structure of ur response json?"
//			);
//
//			System.out.println(response);
//		};
//	}

}
