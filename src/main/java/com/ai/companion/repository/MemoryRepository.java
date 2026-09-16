package com.ai.companion.repository;

import com.ai.companion.entity.Memory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemoryRepository extends JpaRepository<Memory, Long> {

    List<Memory> findByActiveTrue();
}