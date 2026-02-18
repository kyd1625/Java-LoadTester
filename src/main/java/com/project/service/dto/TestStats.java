package com.project.service.dto;

public record TestStats(
        int success,
        int fail,
        int totalRequests,
        double avgLatencyMs,
        double minLatencyMs,
        double maxLatencyMs,
        double p99LatencyMs
) {
}
