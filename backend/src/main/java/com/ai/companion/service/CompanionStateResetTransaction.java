package com.ai.companion.service;

import com.ai.companion.repository.CompanionIdentityRepository;
import com.ai.companion.repository.CompanionMoodRepository;
import com.ai.companion.repository.CompanionProfileRepository;
import com.ai.companion.repository.ConversationMessageRepository;
import com.ai.companion.repository.MemoryRepository;
import com.ai.companion.repository.MoodEventRepository;
import com.ai.companion.repository.ProfileFactRepository;
import com.ai.companion.repository.PersonalFactRepository;
import com.ai.companion.repository.UserProfileRepository;
import com.ai.companion.entity.PersonalFact;
import com.ai.companion.entity.UserProfile;
import com.ai.companion.entity.ConversationMessage;
import com.ai.companion.entity.Memory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class CompanionStateResetTransaction {

    private final CompanionIdentityRepository identityRepository;
    private final ConversationMessageRepository conversationMessageRepository;
    private final MemoryRepository memoryRepository;
    private final ProfileFactRepository profileFactRepository;
    private final CompanionProfileRepository profileRepository;
    private final MoodEventRepository moodEventRepository;
    private final CompanionMoodRepository moodRepository;
    private final PersonalFactRepository personalFactRepository;
    private final UserProfileRepository userProfileRepository;

    public CompanionStateResetTransaction(
            CompanionIdentityRepository identityRepository,
            ConversationMessageRepository conversationMessageRepository,
            MemoryRepository memoryRepository,
            ProfileFactRepository profileFactRepository,
            CompanionProfileRepository profileRepository,
            MoodEventRepository moodEventRepository,
            CompanionMoodRepository moodRepository,
            PersonalFactRepository personalFactRepository,
            UserProfileRepository userProfileRepository) {
        this.identityRepository = identityRepository;
        this.conversationMessageRepository = conversationMessageRepository;
        this.memoryRepository = memoryRepository;
        this.profileFactRepository = profileFactRepository;
        this.profileRepository = profileRepository;
        this.moodEventRepository = moodEventRepository;
        this.moodRepository = moodRepository;
        this.personalFactRepository = personalFactRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional
    public void clearPersistentState() {
        clearCompanionState();
    }

    @Transactional
    public void clearCompanionState() {
        identityRepository.findFirstByActiveTrue().ifPresent(identity ->
                conversationMessageRepository.deleteByIdentityId(identity.getId()));
        profileFactRepository.deleteAllInBatch();
        memoryRepository.deleteAllInBatch();
        moodEventRepository.deleteAllInBatch();
        moodRepository.deleteAllInBatch();
        profileRepository.deleteAllInBatch();

        identityRepository.findFirstByActiveTrue().ifPresent(identity -> {
            identity.setActive(false);
            identityRepository.save(identity);
        });
    }

    @Transactional
    public List<String> clearUserProfile() {
        UserProfile profile = userProfileRepository.findFirstByOrderByIdAsc().orElse(null);
        List<PersonalFact> personalFacts = personalFactRepository.findAll();
        List<String> privateText = new ArrayList<>();
        if (profile != null) {
            privateText.addAll(java.util.Arrays.asList(
                    profile.getName(), profile.getPreferredNickname(), profile.getBio(),
                    profile.getInterests(), profile.getFavoriteTopics(),
                    profile.getCommunicationPreferences(), profile.getThingsToKnow(),
                    profile.getTopicsToAvoid(), profile.getGender(),
                    profile.getAge() == null ? null : profile.getAge().toString()));
        }
        personalFacts.stream().map(PersonalFact::getContent).forEach(privateText::add);
        List<String> redactionTerms = privateText.stream()
                .filter(value -> value != null && value.trim().length() >= 2)
                .map(String::trim).distinct()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();
        if (!redactionTerms.isEmpty()) {
            List<ConversationMessage> messages = conversationMessageRepository.findAll();
            messages.forEach(message -> message.setContent(redact(message.getContent(), redactionTerms)));
            conversationMessageRepository.saveAll(messages);

            List<Memory> affectedMemories = memoryRepository.findAll().stream()
                    .filter(memory -> containsAny(memory.getContent(), redactionTerms)
                            || containsAny(memory.getRawExcerpt(), redactionTerms))
                    .toList();
            if (!affectedMemories.isEmpty()) {
                memoryRepository.deleteAllInBatch(affectedMemories);
            }
        }
        personalFactRepository.deleteAllInBatch();
        userProfileRepository.deleteAllInBatch();
        return redactionTerms;
    }

    @Transactional
    public List<String> clearAll() {
        clearCompanionState();
        return clearUserProfile();
    }

    @Transactional
    public List<String> clearEverything() {
        List<String> portraitIds = identityRepository.findAllByPortraitIdIsNotNull()
                .stream().map(identity -> identity.getPortraitId()).toList();
        conversationMessageRepository.deleteAllInBatch();
        profileFactRepository.deleteAllInBatch();
        memoryRepository.deleteAllInBatch();
        moodEventRepository.deleteAllInBatch();
        moodRepository.deleteAllInBatch();
        profileRepository.deleteAllInBatch();
        personalFactRepository.deleteAllInBatch();
        userProfileRepository.deleteAllInBatch();
        identityRepository.deleteAllInBatch();
        return portraitIds;
    }

    private boolean containsAny(String text, List<String> terms) {
        return text != null && terms.stream()
                .anyMatch(term -> textPattern(term).matcher(text).find());
    }

    private String redact(String text, List<String> terms) {
        if (text == null) return null;
        String result = text;
        for (String term : terms) {
            result = textPattern(term).matcher(result).replaceAll("[removed]");
        }
        return result;
    }

    private Pattern textPattern(String term) {
        return Pattern.compile(
                "(?<![\\p{L}\\p{N}_])" + Pattern.quote(term)
                        + "(?![\\p{L}\\p{N}_])",
                Pattern.CASE_INSENSITIVE);
    }
}
