package com.ai.companion.service;

import com.ai.companion.entity.CompanionProfile;
import com.ai.companion.entity.ProfileFact;
import com.ai.companion.repository.CompanionProfileRepository;
import com.ai.companion.repository.ProfileFactRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProfileFactRetrievalService {

    private final ProfileFactRepository profileFactRepository;
    private final CompanionProfileRepository profileRepository;
    private final EmbeddingService embeddingService;

    public ProfileFactRetrievalService(
            ProfileFactRepository profileFactRepository,
            CompanionProfileRepository profileRepository,
            EmbeddingService embeddingService) {

        this.profileFactRepository = profileFactRepository;
        this.profileRepository = profileRepository;
        this.embeddingService = embeddingService;
    }

    public List<ProfileFact> retrieveRelevantFacts(
            String query,
            int limit) {

        CompanionProfile profile =
                profileRepository.findByActiveTrue()
                        .orElse(null);

        if (profile == null) {
            return List.of();
        }

        List<Float> embedding =
                embeddingService.embed(query);

        String vector =
                embedding.toString();

        return profileFactRepository.findSimilarFacts(
                profile.getId(),
                vector,
                0.45,
                limit
        );
    }
}