package com.ai.companion.service;

import com.ai.companion.entity.CompanionProfile;
import com.ai.companion.entity.FactType;
import com.ai.companion.entity.ProfileFact;
import com.ai.companion.repository.CompanionProfileRepository;
import com.ai.companion.repository.ProfileFactRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ProfileFactService {

    private final ProfileFactRepository profileFactRepository;
    private final CompanionProfileRepository profileRepository;
    private final EmbeddingService embeddingService;

    public ProfileFactService(
            ProfileFactRepository profileFactRepository,
            CompanionProfileRepository profileRepository,
            EmbeddingService embeddingService) {

        this.profileFactRepository = profileFactRepository;
        this.profileRepository = profileRepository;
        this.embeddingService = embeddingService;
    }

    public ProfileFact saveFact(
            FactType type,
            String content,
            String sourceUrl) {

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException(
                    "Profile fact content cannot be empty"
            );
        }

        CompanionProfile profile =
                profileRepository.findByActiveTrue()
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No active companion profile exists"
                                )
                        );

        List<Float> embedding =
                embeddingService.embed(content);

        float[] vector =
                new float[embedding.size()];

        for (int i = 0; i < embedding.size(); i++) {
            vector[i] = embedding.get(i);
        }

        ProfileFact fact = new ProfileFact();

        fact.setProfile(profile);
        fact.setType(type);
        fact.setContent(content);
        fact.setSourceUrl(sourceUrl);
        fact.setSourceRetrievedAt(Instant.now());
        fact.setEmbedding(vector);

        return profileFactRepository.save(fact);
    }

    public List<ProfileFact> getFactsForActiveProfile() {

        CompanionProfile profile =
                profileRepository.findByActiveTrue()
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No active companion profile exists"
                                )
                        );

        return profileFactRepository.findByProfileId(
                profile.getId()
        );
    }
}