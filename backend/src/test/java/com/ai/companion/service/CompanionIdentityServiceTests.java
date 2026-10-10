package com.ai.companion.service;

import com.ai.companion.dto.CharacterProfile;
import com.ai.companion.dto.CompanionInitializationRequest;
import com.ai.companion.dto.WebSearchResult;
import com.ai.companion.entity.CompanionIdentity;
import com.ai.companion.repository.CompanionIdentityRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CompanionIdentityServiceTests {

    @Test
    void createsIdentityWithoutRelationshipField() {
        CompanionIdentityRepository repository = mock(CompanionIdentityRepository.class);
        when(repository.findFirstByActiveTrue()).thenReturn(Optional.empty());
        when(repository.save(any(CompanionIdentity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        CompanionIdentityService service = new CompanionIdentityService(repository);

        CompanionIdentity identity = service.initialize(new CompanionInitializationRequest(
                "Companion", "Personality", "Background", "History",
                "Speech", "Values"));

        assertEquals("Companion", identity.getName());
        verify(repository).save(any(CompanionIdentity.class));
    }

    @Test
    void researchProfileContainsOnlyIdentityFieldsAndExcludesRelationshipsFromSearch() {
        WebSearchService webSearch = mock(WebSearchService.class);
        OllamaService ollama = mock(OllamaService.class);
        List<WebSearchResult> sources = List.of();
        when(webSearch.search(any(String.class), eq(10))).thenReturn(sources);
        when(ollama.buildCharacterProfile("Companion", sources)).thenReturn(
                new CharacterProfile(
                        "Companion", "Personality", "Background", "History",
                        "Speech", "Values"));
        CharacterResearchService service = new CharacterResearchService(webSearch, ollama);

        CharacterProfile profile = service.research("Companion");

        verify(webSearch).search(
                argThat(query -> !query.toLowerCase().contains("relationship")),
                eq(10));
        assertEquals("Companion", profile.name());
        assertEquals("Values", profile.values());
    }
}
