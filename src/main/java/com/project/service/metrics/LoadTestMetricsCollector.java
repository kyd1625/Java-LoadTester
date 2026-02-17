package com.project.service.metrics;

import com.project.service.dto.TestStats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;

public class LoadTestMetricsCollector {

    // 요청 결과 집계값
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failCount = new AtomicInteger(0);

    // latency 집계용 누적기
    private final LongAdder latencySum = new LongAdder();
    private final LongAccumulator minLatency = new LongAccumulator(Long::min, Long.MAX_VALUE);
    private final LongAccumulator maxLatency = new LongAccumulator(Long::max, 0L);

    // P99 계산을 위해 요청별 latency 원본 보관
    private final List<Long> latencies = Collections.synchronizedList(new ArrayList<>());

    public void recordSuccess(long latencyMs) {
        successCount.incrementAndGet();
        recordLatency(latencyMs);
    }

    public void recordFailure(long latencyMs) {
        failCount.incrementAndGet();
        recordLatency(latencyMs);
    }

    public TestStats toStats() {
        int success = successCount.get();
        int fail = failCount.get();
        int totalRequests = success + fail;

        // 요청이 한 건도 없으면 0으로 초기화된 통계 반환
        if (totalRequests == 0) {
            return new TestStats(0, 0, 0, 0.0, 0.0, 0.0, 0.0);
        }

        double avgLatencyMs = (double) latencySum.sum() / totalRequests;
        double minLatencyMs = (double) minLatency.get();
        double maxLatencyMs = (double) maxLatency.get();
        double p99LatencyMs = calculateP99(latencies);

        return new TestStats(success, fail, totalRequests, avgLatencyMs, minLatencyMs, maxLatencyMs, p99LatencyMs);
    }

    private void recordLatency(long latencyMs) {
        latencySum.add(latencyMs);
        minLatency.accumulate(latencyMs);
        maxLatency.accumulate(latencyMs);
        latencies.add(latencyMs);
    }

    private double calculateP99(List<Long> latencyValues) {
        if (latencyValues.isEmpty()) {
            return 0.0;
        }

        // 오름차순 정렬 후 99% 지점 인덱스를 선택
        List<Long> sorted = new ArrayList<>(latencyValues);
        Collections.sort(sorted);
        int index = (int) Math.ceil(sorted.size() * 0.99) - 1;
        index = Math.max(index, 0);
        return sorted.get(index);
    }
}
