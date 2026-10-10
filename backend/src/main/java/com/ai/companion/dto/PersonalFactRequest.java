package com.ai.companion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PersonalFactRequest(@NotBlank @Size(max = 2000) String content) {
}
