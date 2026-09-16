package com.ai.companion;

import com.ai.companion.entity.Memory;
import com.ai.companion.entity.MemoryCategory;
import com.ai.companion.entity.MemorySource;
import com.ai.companion.service.MemoryService;
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
//	CommandLineRunner testMemory(MemoryService memoryService) {
//		return args -> {
//
//			Memory memory = new Memory();
//
//			memory.setContent("User loves astronomy.");
//			memory.setRawExcerpt("I really love astronomy.");
//			memory.setCategory(MemoryCategory.INTEREST); // we'll fix this
//			memory.setImportance(8);
//			memory.setConfidence(0.95);
//			memory.setSource(MemorySource.EXTRACTED);
//
//			memoryService.save(memory);
//		};
//	}

}
