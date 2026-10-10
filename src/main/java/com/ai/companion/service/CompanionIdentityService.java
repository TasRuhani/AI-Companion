package com.ai.companion.service;

import com.ai.companion.entity.CompanionIdentity;
import com.ai.companion.repository.CompanionIdentityRepository;
import org.springframework.stereotype.Service;
import com.ai.companion.dto.CompanionInitializationRequest;

@Service
public class CompanionIdentityService {

    private final CompanionIdentityRepository identityRepository;

    public CompanionIdentityService(
            CompanionIdentityRepository identityRepository) {

        this.identityRepository = identityRepository;
    }

    public CompanionIdentity initialize(
            CompanionInitializationRequest request) {

        if (exists()) {
            throw new IllegalStateException(
                    "Companion identity already exists."
            );
        }

        CompanionIdentity identity =
                new CompanionIdentity();

        identity.setName(
                request.name()
        );

        identity.setPersonality(
                request.personality()
        );

        identity.setHistory(
                request.history()
        );

        identity.setBackground(
                request.background()
        );

        identity.setSpeechStyle(
                request.speechStyle()
        );

        identity.setValues(
                request.values()
        );

        identity.setRelationships(
                request.relationships()
        );

        identity.setSource(
                com.ai.companion.entity.IdentitySource.USER_CREATED
        );

        identity.setActive(true);

        return identityRepository.save(identity);
    }

    public boolean exists() {

        return identityRepository
                .findFirstByActiveTrue()
                .isPresent();
    }

    public CompanionIdentity getIdentity() {

        return identityRepository
                .findFirstByActiveTrue()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No active companion identity exists."
                        )
                );
    }

    public CompanionIdentity save(
            CompanionIdentity identity) {

        return identityRepository.save(identity);
    }

    public void reset() {

        identityRepository
                .findFirstByActiveTrue()
                .ifPresent(identity -> {

                    identity.setActive(false);

                    identityRepository.save(identity);
                });
    }
}