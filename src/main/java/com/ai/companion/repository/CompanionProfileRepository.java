package com.ai.companion.repository;

import com.ai.companion.entity.CompanionProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanionProfileRepository
        extends JpaRepository<CompanionProfile, Long> {

    Optional<CompanionProfile> findByActiveTrue();
}