package com.ai.companion.controller;

import com.ai.companion.dto.*;
import com.ai.companion.repository.CompanionIdentityRepository;
import com.ai.companion.service.SettingsService;
import com.ai.companion.service.CompanionResetService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;
    private final CompanionIdentityRepository identityRepository;
    private final CompanionResetService resetService;

    public SettingsController(
            SettingsService settingsService,
            CompanionIdentityRepository identityRepository,
            CompanionResetService resetService) {
        this.settingsService = settingsService;
        this.identityRepository = identityRepository;
        this.resetService = resetService;
    }

    @GetMapping("/onboarding")
    public Map<String, Object> onboarding() {
        boolean profileComplete = settingsService.getProfile() != null;
        boolean identityComplete = identityRepository.findFirstByActiveTrue().isPresent();
        String nextStep = !profileComplete ? "USER_PROFILE"
                : !identityComplete ? "COMPANION_IDENTITY" : "READY";
        return Map.of("userProfileComplete", profileComplete,
                "companionIdentityComplete", identityComplete, "nextStep", nextStep);
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile() {
        UserProfileResponse profile = settingsService.getProfile();
        return profile == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    public UserProfileResponse saveProfile(@Valid @RequestBody UserProfileRequest request) {
        return settingsService.saveProfile(request);
    }

    @GetMapping("/personal-facts")
    public List<PersonalFactResponse> getFacts() {
        return settingsService.getFacts();
    }

    @PostMapping("/personal-facts")
    public PersonalFactResponse addFact(@Valid @RequestBody PersonalFactRequest request) {
        return settingsService.addFact(request);
    }

    @PutMapping("/personal-facts/{factId}")
    public PersonalFactResponse updateFact(
            @PathVariable Long factId, @Valid @RequestBody PersonalFactRequest request) {
        return settingsService.updateFact(factId, request);
    }

    @PatchMapping("/personal-facts/{factId}")
    public PersonalFactResponse setFactStatus(
            @PathVariable Long factId, @Valid @RequestBody PersonalFactStatusRequest request) {
        return settingsService.setFactStatus(factId, request);
    }

    @DeleteMapping("/personal-facts/{factId}")
    public ResponseEntity<Void> deleteFact(@PathVariable Long factId) {
        settingsService.deleteFact(factId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-profile")
    public ResponseEntity<Void> resetProfile() {
        resetService.resetUserProfile();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-all")
    public ResponseEntity<Void> resetAll() {
        resetService.resetAll();
        return ResponseEntity.noContent().build();
    }
}
