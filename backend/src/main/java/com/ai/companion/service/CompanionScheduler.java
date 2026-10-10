package com.ai.companion.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CompanionScheduler {

    private final ProactiveMessageService proactiveMessageService;

    public CompanionScheduler(
            ProactiveMessageService proactiveMessageService) {

        this.proactiveMessageService = proactiveMessageService;
    }

    @Scheduled(fixedRate = 10000)
    public void checkForProactiveMessage() {
        proactiveMessageService.check();
    }
}