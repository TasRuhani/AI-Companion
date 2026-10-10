package com.ai.companion.repository;

import com.ai.companion.entity.ProfileFact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProfileFactRepository
        extends JpaRepository<ProfileFact, Long> {

    List<ProfileFact> findByProfileId(Long profileId);

    @Query(value = """
            SELECT *
            FROM profile_facts
            WHERE profile_id = :profileId
              AND embedding IS NOT NULL
              AND embedding <=> CAST(:embedding AS vector) <= :threshold
            ORDER BY embedding <=> CAST(:embedding AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<ProfileFact> findSimilarFacts(
            @Param("profileId") Long profileId,
            @Param("embedding") String embedding,
            @Param("threshold") double threshold,
            @Param("limit") int limit
    );
}