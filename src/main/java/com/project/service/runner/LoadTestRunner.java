package com.project.service.runner;

import com.project.model.LoadTestScenario;
import com.project.service.client.LoadTestRequestExecutor;
import com.project.service.dto.RequestResult;
import com.project.service.dto.TestStats;
import com.project.service.metrics.LoadTestMetricsCollector;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class LoadTestRunner {

    private final LoadTestRequestExecutor requestExecutor;

    public LoadTestRunner(LoadTestRequestExecutor requestExecutor) {
        this.requestExecutor = requestExecutor;
    }

    public TestStats run(LoadTestScenario scenario, long resultId) {
        // 모든 스레드가 공유하는 글로벌 요청 순번
        AtomicLong globalOrder = new AtomicLong(0);
        // 성공/실패 카운트 + latency 통계를 누적하는 집계기
        LoadTestMetricsCollector metricsCollector = new LoadTestMetricsCollector();

        // 종료시각 = 현재시각 + 테스트 지속시간(초)
        long endTimeMillis = System.currentTimeMillis() + (scenario.getDurationSeconds() * 1000L);

        // Virtual Thread Worker 개수만큼 태스크 생성
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < scenario.getVirtualThreadCount(); i++) {
                executor.submit(() -> {
                    // 종료시각 전까지 요청 반복 실행
                    while (System.currentTimeMillis() < endTimeMillis) {
                        long currentOrder = globalOrder.incrementAndGet();
                        RequestResult requestResult = requestExecutor.execute(scenario, resultId, currentOrder);

                        // 요청 결과를 성공/실패 + latency로 집계
                        if (requestResult.success()) {
                            metricsCollector.recordSuccess(requestResult.latencyMs());
                        } else {
                            metricsCollector.recordFailure(requestResult.latencyMs());
                        }
                    }
                });
            }
        }

        // 누적된 집계를 최종 통계 객체로 변환
        return metricsCollector.toStats();
    }
}
