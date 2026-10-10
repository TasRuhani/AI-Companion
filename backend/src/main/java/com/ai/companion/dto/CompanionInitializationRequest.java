package com.ai.companion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanionInitializationRequest(

        @NotBlank @Size(max = 120) String name,

        @NotBlank @Size(max = 10000) String personality,

        @NotBlank @Size(max = 10000) String background,

        @NotBlank @Size(max = 10000) String history,

        @NotBlank @Size(max = 10000) String speechStyle,

        @NotBlank @Size(max = 10000) String values
) {
}