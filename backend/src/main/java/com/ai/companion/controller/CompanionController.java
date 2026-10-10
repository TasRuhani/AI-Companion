package com.ai.companion.controller;

import com.ai.companion.dto.CharacterProfile;
import com.ai.companion.dto.CompanionInitializationRequest;
import com.ai.companion.dto.CompanionResearchRequest;
import com.ai.companion.entity.CompanionIdentity;
import com.ai.companion.service.CharacterResearchService;
import com.ai.companion.service.CompanionIdentityService;
import com.ai.companion.service.CompanionPortraitService;
import com.ai.companion.service.CompanionResetService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/companion")
public class CompanionController {

    private final CompanionIdentityService identityService;
    private final CharacterResearchService characterResearchService;
    private final CompanionPortraitService portraitService;
    private final CompanionResetService resetService;

    public CompanionController(
            CompanionIdentityService identityService,
            CharacterResearchService characterResearchService,
            CompanionPortraitService portraitService,
            CompanionResetService resetService) {

        this.identityService = identityService;
        this.characterResearchService = characterResearchService;
        this.portraitService = portraitService;
        this.resetService = resetService;
    }

    @GetMapping("/identity")
    public CompanionIdentity getIdentity() {

        return identityService.getIdentity();
    }

    @PostMapping("/initialize")
    public CompanionIdentity initialize(
            @Valid @RequestBody CompanionInitializationRequest request) {

        return identityService.initialize(request);
    }

    @PutMapping("/identity")
    public CompanionIdentity updateIdentity(
            @Valid @RequestBody CompanionInitializationRequest request) {
        return identityService.updateActive(request);
    }

    @PostMapping("/reset")
    public String reset() {

        resetService.reset();

        return "Companion state reset. Create an identity before starting a conversation.";
    }

    @PostMapping("/reset-companion")
    public ResponseEntity<Void> resetCompanion() {
        resetService.resetCompanion();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/research")
    public CharacterProfile research(
            @RequestBody CompanionResearchRequest request) {

        return characterResearchService.research(
                request.character()
        );
    }

    @PostMapping(
            value = "/identity/{identityId}/portrait",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CompanionIdentity uploadPortrait(
            @PathVariable Long identityId,
            @RequestPart("file") MultipartFile file) {
        return portraitService.upload(identityId, file);
    }

    @GetMapping("/identity/{identityId}/portrait")
    public ResponseEntity<Resource> getPortrait(@PathVariable Long identityId) {
        Path path = portraitService.getPortrait(identityId);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).cachePrivate().mustRevalidate())
                .header(HttpHeaders.ETAG, "\"" + path.getFileName().toString().replace(".png", "") + "\"")
                .body(new FileSystemResource(path));
    }

    @DeleteMapping("/identity/{identityId}/portrait")
    public ResponseEntity<Void> removePortrait(@PathVariable Long identityId) {
        portraitService.remove(identityId);
        return ResponseEntity.noContent().build();
    }
}