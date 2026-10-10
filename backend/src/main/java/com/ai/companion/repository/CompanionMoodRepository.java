package com.ai.companion.repository;

import com.ai.companion.entity.CompanionMood;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanionMoodRepository
        extends JpaRepository<CompanionMood, Long> {
}