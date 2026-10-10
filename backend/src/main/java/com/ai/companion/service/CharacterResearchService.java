package com.ai.companion.service;

import com.ai.companion.dto.CharacterProfile;
import com.ai.companion.dto.WebSearchResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CharacterResearchService {

    private final WebSearchService webSearchService;
    private final OllamaService ollamaService;

    public CharacterResearchService(
            WebSearchService webSearchService,
            OllamaService ollamaService) {

        this.webSearchService = webSearchService;
        this.ollamaService = ollamaService;
    }

    public CharacterProfile research(
            String characterName) {

        List<WebSearchResult> results =
                webSearchService.search(
                        characterName
                                + " personality background history speech style values",
                        10
                );

        CharacterProfile researchedProfile = ollamaService.buildCharacterProfile(
                characterName,
                results
        );
        return new CharacterProfile(
                researchedProfile.name(),
                researchedProfile.personality(),
                researchedProfile.background(),
                researchedProfile.history(),
                researchedProfile.speechStyle(),
                researchedProfile.values()
        );
    }
}