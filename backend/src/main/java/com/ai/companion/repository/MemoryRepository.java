package com.ai.companion.repository;

import com.ai.companion.entity.Memory;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MemoryRepository extends JpaRepository<Memory, Long> {

    List<Memory> findByActiveTrue();

    @Query(value = """
        SELECT *
        FROM memories
        WHERE active = true
          AND embedding IS NOT NULL
          AND embedding <=> CAST(:embedding AS vector) <= :threshold
        ORDER BY embedding <=> CAST(:embedding AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<Memory> findSimilarActiveMemories(
            @Param("embedding") String embedding,
            @Param("threshold") double threshold,
            @Param("limit") int limit
    );
}