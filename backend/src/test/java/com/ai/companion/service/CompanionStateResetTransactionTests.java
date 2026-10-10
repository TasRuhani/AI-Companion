package com.ai.companion.service;

import com.ai.companion.entity.ConversationMessage;
import com.ai.companion.entity.Memory;
import com.ai.companion.entity.PersonalFact;
import com.ai.companion.entity.UserProfile;
import com.ai.companion.repository.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class CompanionStateResetTransactionTests {

    @Test
    void profileResetRedactsHistoryAndDeletesOnlyMatchingMemories() {
        CompanionIdentityRepository identities = mock(CompanionIdentityRepository.class);
        ConversationMessageRepository messages = mock(ConversationMessageRepository.class);
        MemoryRepository memories = mock(MemoryRepository.class);
        ProfileFactRepository companionFacts = mock(ProfileFactRepository.class);
        CompanionProfileRepository companionProfiles = mock(CompanionProfileRepository.class);
        MoodEventRepository moodEvents = mock(MoodEventRepository.class);
        CompanionMoodRepository moods = mock(CompanionMoodRepository.class);
        PersonalFactRepository personalFacts = mock(PersonalFactRepository.class);
        UserProfileRepository userProfiles = mock(UserProfileRepository.class);

        UserProfile profile = new UserProfile();
        profile.setName("Mira");
        profile.setPreferredNickname("Mimi");
        PersonalFact fact = new PersonalFact();
        fact.setContent("tea");
        when(userProfiles.findFirstByOrderByIdAsc()).thenReturn(Optional.of(profile));
        when(personalFacts.findAll()).thenReturn(List.of(fact));

        ConversationMessage message = new ConversationMessage();
        message.setContent("Hi Mira, I remember that you prefer tea.");
        when(messages.findAll()).thenReturn(List.of(message));
        Memory matchingMemory = new Memory();
        matchingMemory.setContent("User prefers tea.");
        Memory unrelatedMemory = new Memory();
        unrelatedMemory.setContent("User likes racing.");
        when(memories.findAll()).thenReturn(List.of(matchingMemory, unrelatedMemory));

        CompanionStateResetTransaction transaction = new CompanionStateResetTransaction(
                identities, messages, memories, companionFacts, companionProfiles,
                moodEvents, moods, personalFacts, userProfiles);

        transaction.clearUserProfile();

        assertEquals("Hi [removed], I remember that you prefer [removed].", message.getContent());
        verify(messages).saveAll(List.of(message));
        verify(memories).deleteAllInBatch(List.of(matchingMemory));
        verify(personalFacts).deleteAllInBatch();
        verify(userProfiles).deleteAllInBatch();
    }
}
