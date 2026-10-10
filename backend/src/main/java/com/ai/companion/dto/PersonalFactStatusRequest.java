package com.ai.companion.dto;

import com.ai.companion.entity.PersonalFactStatus;
import jakarta.validation.constraints.NotNull;

public record PersonalFactStatusRequest(@NotNull PersonalFactStatus status) {
}
