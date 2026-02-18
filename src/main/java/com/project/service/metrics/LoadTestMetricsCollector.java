package com.project.service.metrics;

import com.project.service.dto.TestStats;
import org.HdrHistogram.AtomicHistogram;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;

public class LoadTestMetricsCollector {

    // 요청 결과 카운터
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failCount = new AtomicInteger(0);

    // 지연시간 집계값
    private final LongAdder latencySum = new LongAdder();
    private final LongAccumulator minLatency = new LongAccumulator(Long::min, Long.MAX_VALUE);
    private final LongAccumulator maxLatency = new LongAccumulator(Long::max, 0L);

    private static final long MAX_TRACKABLE_LATENCY_MS = 3_600_000L; // 1시간
    private final AtomicHistogram latencyHistogram = new AtomicHistogram(MAX_TRACKABLE_LATENCY_MS, 3);

    /*
     * 기존 방식:
     * 모든 latency를 List에 저장한 뒤 정렬해서 P99를 계산했습니다.
     * 이 방식은 요청 수가 늘어날수록 메모리가 선형 증가하여 고부하에서 OOM 위험이 있습니다.
     *
     * private final List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
     */

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

        if (totalRequests == 0) {
            return new TestStats(0, 0, 0, 0.0, 0.0, 0.0, 0.0);
        }

        double avgLatencyMs = (double) latencySum.sum() / totalRequests;
        double minLatencyMs = (double) minLatency.get();
        double maxLatencyMs = (double) maxLatency.get();
        double p99LatencyMs = latencyHistogram.getValueAtPercentile(99.0);

        return new TestStats(success, fail, totalRequests, avgLatencyMs, minLatencyMs, maxLatencyMs, p99LatencyMs);
    }

    private void recordLatency(long latencyMs) {
        long boundedLatencyMs = Math.max(0L, Math.min(latencyMs, MAX_TRACKABLE_LATENCY_MS));

        latencySum.add(boundedLatencyMs);
        minLatency.accumulate(boundedLatencyMs);
        maxLatency.accumulate(boundedLatencyMs);
        latencyHistogram.recordValue(boundedLatencyMs);

        /*
         * 기존 방식:
         * latencies.add(latencyMs);
         */
    }

    /*
     * 기존 방식:
     * private double calculateP99(List<Long> latencyValues) {
     *     if (latencyValues.isEmpty()) {
     *         return 0.0;
     *     }
     *     List<Long> sorted = new ArrayList<>(latencyValues);
     *     Collections.sort(sorted);
     *     int index = (int) Math.ceil(sorted.size() * 0.99) - 1;
     *     index = Math.max(index, 0);
     *     return sorted.get(index);
     * }
     */
}
