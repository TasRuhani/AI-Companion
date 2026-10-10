package com.ai.companion.service;

import com.ai.companion.entity.CompanionIdentity;
import com.ai.companion.repository.CompanionIdentityRepository;
import org.springframework.stereotype.Service;
import com.ai.companion.dto.CompanionInitializationRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "No active companion identity exists."
                        )
                );
    }

    public CompanionIdentity save(
            CompanionIdentity identity) {

        return identityRepository.save(identity);
    }

    public CompanionIdentity updateActive(
            CompanionInitializationRequest request) {
        CompanionIdentity identity = identityRepository.findFirstByActiveTrue()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No active companion identity exists."));
        identity.setName(request.name());
        identity.setPersonality(request.personality());
        identity.setBackground(request.background());
        identity.setHistory(request.history());
        identity.setSpeechStyle(request.speechStyle());
        identity.setValues(request.values());
        return identityRepository.save(identity);
    }

}