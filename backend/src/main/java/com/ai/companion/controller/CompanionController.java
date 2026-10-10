package com.ai.companion.controller;

import com.ai.companion.dto.CharacterProfile;
import com.ai.companion.dto.CompanionInitializationRequest;
import com.ai.companion.dto.CompanionResearchRequest;
import com.ai.companion.entity.CompanionIdentity;
import com.ai.companion.service.CharacterResearchService;
import com.ai.companion.service.CompanionIdentityService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/companion")
public class CompanionController {

    private final CompanionIdentityService identityService;
    private final CharacterResearchService characterResearchService;

    public CompanionController(
            CompanionIdentityService identityService, CharacterResearchService characterResearchService) {

        this.identityService = identityService;
        this.characterResearchService = characterResearchService;
    }

    @GetMapping("/identity")
    public CompanionIdentity getIdentity() {

        return identityService.getIdentity();
    }

    @PostMapping("/initialize")
    public CompanionIdentity initialize(
            @RequestBody CompanionInitializationRequest request) {

        return identityService.initialize(request);
    }

    @PostMapping("/reset")
    public String reset() {

        identityService.reset();

        return "Companion identity reset.";
    }

    @PostMapping("/research")
    public CharacterProfile research(
            @RequestBody CompanionResearchRequest request) {

//        CharacterProfile profile =
//                characterResearchService.research(
//                        request.character()
//                );

//        CompanionInitializationRequest initialization =
//                new CompanionInitializationRequest(
//                        profile.name(),
//                        profile.personality(),
//                        profile.background(),
//                        profile.history(),
//                        profile.speechStyle(),
//                        profile.values(),
//                        profile.relationships()
//                );

        return characterResearchService.research(
                request.character()
        );
    }
}