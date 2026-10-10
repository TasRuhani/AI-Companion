package com.ai.companion.service;

import com.ai.companion.dto.*;
import com.ai.companion.entity.PersonalFact;
import com.ai.companion.entity.PersonalFactStatus;
import com.ai.companion.entity.UserProfile;
import com.ai.companion.repository.PersonalFactRepository;
import com.ai.companion.repository.UserProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class SettingsService {

    private final UserProfileRepository profileRepository;
    private final PersonalFactRepository factRepository;

    public SettingsService(
            UserProfileRepository profileRepository,
            PersonalFactRepository factRepository) {
        this.profileRepository = profileRepository;
        this.factRepository = factRepository;
    }

    public UserProfileResponse getProfile() {
        return profileRepository.findFirstByOrderByIdAsc()
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional
    public UserProfileResponse saveProfile(UserProfileRequest request) {
        UserProfile profile = profileRepository.findFirstByOrderByIdAsc()
                .orElseGet(UserProfile::new);
        profile.setName(request.name().trim());
        profile.setPreferredNickname(trimToNull(request.preferredNickname()));
        profile.setAge(request.age());
        profile.setGender(trimToNull(request.gender()));
        profile.setBio(trimToNull(request.bio()));
        profile.setInterests(trimToNull(request.interests()));
        profile.setFavoriteTopics(trimToNull(request.favoriteTopics()));
        profile.setCommunicationPreferences(trimToNull(request.communicationPreferences()));
        profile.setThingsToKnow(trimToNull(request.thingsToKnow()));
        profile.setTopicsToAvoid(trimToNull(request.topicsToAvoid()));
        return toResponse(profileRepository.save(profile));
    }

    public List<PersonalFactResponse> getFacts() {
        return factRepository.findByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public PersonalFactResponse addFact(PersonalFactRequest request) {
        String content = request.content().trim();
        if (factRepository.existsByContentIgnoreCase(content)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "This personal fact already exists.");
        }
        PersonalFact fact = new PersonalFact();
        fact.setContent(content);
        fact.setStatus(PersonalFactStatus.PROPOSED);
        fact.setSource("MANUAL");
        return toResponse(factRepository.save(fact));
    }

    @Transactional
    public void proposeConversationFact(String content) {
        String candidate = content == null ? "" : content.trim();
        if (candidate.isBlank() || factRepository.existsByContentIgnoreCase(candidate)) {
            return;
        }
        PersonalFact fact = new PersonalFact();
        fact.setContent(candidate);
        fact.setStatus(PersonalFactStatus.PROPOSED);
        fact.setSource("CONVERSATION");
        factRepository.save(fact);
    }

    @Transactional
    public PersonalFactResponse updateFact(
            Long factId, PersonalFactRequest request) {
        PersonalFact fact = findFact(factId);
        fact.setContent(request.content().trim());
        fact.setStatus(PersonalFactStatus.PROPOSED);
        return toResponse(factRepository.save(fact));
    }

    @Transactional
    public PersonalFactResponse setFactStatus(
            Long factId, PersonalFactStatusRequest request) {
        PersonalFact fact = findFact(factId);
        fact.setStatus(request.status());
        return toResponse(factRepository.save(fact));
    }

    @Transactional
    public void deleteFact(Long factId) {
        factRepository.delete(findFact(factId));
    }

    private PersonalFact findFact(Long id) {
        return factRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Personal fact not found."));
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private UserProfileResponse toResponse(UserProfile profile) {
        return new UserProfileResponse(profile.getId(), profile.getName(),
                profile.getPreferredNickname(), profile.getAge(), profile.getGender(),
                profile.getBio(), profile.getInterests(), profile.getFavoriteTopics(),
                profile.getCommunicationPreferences(), profile.getThingsToKnow(),
                profile.getTopicsToAvoid(), profile.getCreatedAt(), profile.getUpdatedAt());
    }

    private PersonalFactResponse toResponse(PersonalFact fact) {
        String reason = switch (fact.getStatus()) {
            case PROPOSED -> "CONVERSATION".equals(fact.getSource())
                    ? "Suggested from a conversation; waiting for your review."
                    : "Added by you; confirm it to include it in companion responses.";
            case CONFIRMED -> "Confirmed by you.";
            case REJECTED -> "Rejected by you.";
        };
        return new PersonalFactResponse(fact.getId(), fact.getContent(), fact.getStatus(),
                fact.getSource(), reason, fact.getCreatedAt(), fact.getUpdatedAt());
    }
}
