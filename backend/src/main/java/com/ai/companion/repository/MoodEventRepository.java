package com.ai.companion.repository;

import com.ai.companion.entity.MoodEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MoodEventRepository
        extends JpaRepository<MoodEvent, Long> {
}