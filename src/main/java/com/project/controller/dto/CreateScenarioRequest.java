package com.project.controller.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateScenarioRequest(
        @NotBlank(message = "name은 필수입니다.")
        @Size(max = 100, message = "name은 100자 이하여야 합니다.")
        String name,

        @NotBlank(message = "targetUrl은 필수입니다.")
        @Size(max = 255, message = "targetUrl은 255자 이하여야 합니다.")
        String targetUrl,

        @NotBlank(message = "httpMethod는 필수입니다.")
        @Pattern(regexp = "GET|POST|PUT|DELETE|PATCH", message = "httpMethod는 GET, POST, PUT, DELETE, PATCH 중 하나여야 합니다.")
        String httpMethod,

        String requestParams,

        @NotNull(message = "targetTps는 필수입니다.")
        @Min(value = 1, message = "targetTps는 1 이상이어야 합니다.")
        @Max(value = 100000, message = "targetTps는 100000 이하여야 합니다.")
        Integer targetTps,

        @NotNull(message = "virtualThreadCount는 필수입니다.")
        @Min(value = 1, message = "virtualThreadCount는 1 이상이어야 합니다.")
        @Max(value = 100000, message = "virtualThreadCount는 100000 이하여야 합니다.")
        Integer virtualThreadCount,

        @NotNull(message = "durationSeconds는 필수입니다.")
        @Min(value = 1, message = "durationSeconds는 1 이상이어야 합니다.")
        @Max(value = 86400, message = "durationSeconds는 86400 이하여야 합니다.")
        Integer durationSeconds
) {
}
