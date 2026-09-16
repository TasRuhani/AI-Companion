package com.ai.companion.controller;

import com.ai.companion.dto.MemoryRequest;
import com.ai.companion.dto.MemoryResponse;
import com.ai.companion.entity.Memory;
import com.ai.companion.service.MemoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/memories")
public class MemoryController {

    private final MemoryService memoryService;

    public MemoryController(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @PostMapping
    public MemoryResponse createMemory(
            @Valid @RequestBody MemoryRequest request
    ) {
        return memoryService.save(request);
    }

    @GetMapping
    public List<MemoryResponse> getMemories() {
        return memoryService.getAll();
    }
}