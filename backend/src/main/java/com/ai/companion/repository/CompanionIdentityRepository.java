package com.ai.companion.repository;

import com.ai.companion.entity.CompanionIdentity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanionIdentityRepository
        extends JpaRepository<CompanionIdentity, Long> {

    Optional<CompanionIdentity> findFirstByActiveTrue();
}