package com.project.controller.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StartTestRequest(
        @NotNull(message = "scenarioId는 필수입니다.")
        @Positive(message = "scenarioId는 양수여야 합니다.")
        Long scenarioId
) {
}
