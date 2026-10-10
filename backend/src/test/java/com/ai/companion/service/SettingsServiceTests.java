package com.ai.companion.service;

import com.ai.companion.dto.PersonalFactRequest;
import com.ai.companion.dto.UserProfileRequest;
import com.ai.companion.entity.PersonalFact;
import com.ai.companion.entity.PersonalFactStatus;
import com.ai.companion.repository.CompanionIdentityRepository;
import com.ai.companion.repository.CompanionProfileRepository;
import com.ai.companion.entity.UserProfile;
import com.ai.companion.repository.PersonalFactRepository;
import com.ai.companion.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SettingsServiceTests {

    @Test
    void savesTrimmedUserProfileIntoSingleProfileRecord() {
        UserProfileRepository profiles = mock(UserProfileRepository.class);
        PersonalFactRepository facts = mock(PersonalFactRepository.class);
        when(profiles.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
        when(profiles.save(any(UserProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        SettingsService service = new SettingsService(profiles, facts);

        var saved = service.saveProfile(new UserProfileRequest(
                "  Mira  ", "  Mimi ", 28, null, null, null, null,
                "  Gentle and concise  ", null, null));

        assertEquals(1L, saved.id());
        assertEquals("Mira", saved.name());
        assertEquals("Mimi", saved.preferredNickname());
        assertEquals("Gentle and concise", saved.communicationPreferences());
        verify(profiles).save(any(UserProfile.class));
    }

    @Test
    void conversationFactIsProposedAndDuplicateIsNotInsertedAgain() {
        UserProfileRepository profiles = mock(UserProfileRepository.class);
        PersonalFactRepository facts = mock(PersonalFactRepository.class);
        when(facts.existsByContentIgnoreCase("I prefer short replies")).thenReturn(false, true);
        when(facts.save(any(PersonalFact.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        SettingsService service = new SettingsService(profiles, facts);

        service.proposeConversationFact("I prefer short replies");
        service.proposeConversationFact("I prefer short replies");

        var captor = org.mockito.ArgumentCaptor.forClass(PersonalFact.class);
        verify(facts, times(1)).save(captor.capture());
        assertEquals(PersonalFactStatus.PROPOSED, captor.getValue().getStatus());
        assertEquals("CONVERSATION", captor.getValue().getSource());
    }

    @Test
    void manuallyAddedFactsAreProposedUntilConfirmed() {
        UserProfileRepository profiles = mock(UserProfileRepository.class);
        PersonalFactRepository facts = mock(PersonalFactRepository.class);
        when(facts.existsByContentIgnoreCase("I like tea")).thenReturn(false);
        when(facts.save(any(PersonalFact.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        SettingsService service = new SettingsService(profiles, facts);

        var saved = service.addFact(new PersonalFactRequest(" I like tea "));

        assertEquals(PersonalFactStatus.PROPOSED, saved.status());
        assertEquals("MANUAL", saved.source());
        assertTrue(saved.reviewReason().contains("confirm"));
    }

    @Test
    void duplicatePersonalFactIsReportedRatherThanSilentlyDuplicated() {
        UserProfileRepository profiles = mock(UserProfileRepository.class);
        PersonalFactRepository facts = mock(PersonalFactRepository.class);
        when(facts.existsByContentIgnoreCase("I like tea")).thenReturn(true);
        SettingsService service = new SettingsService(profiles, facts);

        assertThrows(ResponseStatusException.class,
                () -> service.addFact(new PersonalFactRequest("I like tea")));
        verify(facts, never()).save(any(PersonalFact.class));
    }

    @Test
    void promptUsesPreferredNameAndOnlyConfirmedFacts() {
        UserProfile profile = new UserProfile();
        profile.setName("Mira");
        profile.setPreferredNickname("Mimi");
        PersonalFact confirmed = new PersonalFact();
        confirmed.setContent("Prefers concise replies");
        PersonalFact proposed = new PersonalFact();
        proposed.setContent("Has an unconfirmed hobby");

        UserProfileRepository profiles = mock(UserProfileRepository.class);
        PersonalFactRepository facts = mock(PersonalFactRepository.class);
        when(profiles.findFirstByOrderByIdAsc()).thenReturn(Optional.of(profile));
        when(facts.findTop100ByStatusOrderByCreatedAtDesc(PersonalFactStatus.CONFIRMED))
                .thenReturn(List.of(confirmed));
        OllamaService ollama = new OllamaService(
                mock(ObjectMapper.class),
                mock(MoodBehaviorService.class),
                mock(CompanionProfileRepository.class),
                mock(ProfileFactRetrievalService.class),
                mock(CompanionIdentityRepository.class),
                profiles,
                facts);

        String prompt = ollama.buildProfileBlock(null);

        assertTrue(prompt.contains("Preferred name: Mimi"));
        assertTrue(prompt.contains("Address the user by their preferred name"));
        assertTrue(prompt.contains("Prefers concise replies"));
        assertFalse(prompt.contains(proposed.getContent()));
    }
}
