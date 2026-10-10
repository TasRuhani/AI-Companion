package com.ai.companion.repository;

import com.ai.companion.entity.PersonalFact;
import com.ai.companion.entity.PersonalFactStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PersonalFactRepository extends JpaRepository<PersonalFact, Long> {
    List<PersonalFact> findByOrderByCreatedAtDesc();
    List<PersonalFact> findByStatusOrderByCreatedAtDesc(PersonalFactStatus status);
    List<PersonalFact> findTop100ByStatusOrderByCreatedAtDesc(PersonalFactStatus status);
    boolean existsByContentIgnoreCase(String content);
}
