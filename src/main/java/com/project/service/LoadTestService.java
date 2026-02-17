package com.project.service;

import com.project.model.LoadTestResult;
import com.project.model.LoadTestScenario;
import com.project.repository.LoadTestResultRepository;
import com.project.repository.LoadTestScenarioRepository;
import com.project.service.dto.TestStats;
import com.project.service.runner.LoadTestRunner;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class LoadTestService {

    private final LoadTestRunner loadTestRunner;
    private final LoadTestResultRepository loadTestResultRepository;
    private final LoadTestScenarioRepository loadTestScenarioRepository;
    private final Clock clock;

    public LoadTestService(
            LoadTestRunner loadTestRunner,
            LoadTestResultRepository loadTestResultRepository,
            LoadTestScenarioRepository loadTestScenarioRepository,
            Clock clock
    ) {
        this.loadTestRunner = loadTestRunner;
        this.loadTestResultRepository = loadTestResultRepository;
        this.loadTestScenarioRepository = loadTestScenarioRepository;
        this.clock = clock;
    }

    public void startTestEngine(LoadTestScenario scenario) {
        // 1. 시나리오 insert 후 생성된 ID 확보
        loadTestScenarioRepository.insertScenario(scenario);

        // 2. 결과 row 선생성 후 생성된 Result ID 확보
        LoadTestResult result = new LoadTestResult();
        result.setScenarioId(scenario.getId());
        result.setStartedAt(LocalDateTime.now(clock));
        loadTestResultRepository.insertResult(result);

        // 3. 가상 스레드 기반 테스트 실행
        TestStats stats = loadTestRunner.run(scenario, result.getId());

        // 4. 테스트 완료 후 집계 결과/성능 지표 최종 업데이트
        result.setSuccessCount(stats.success());
        result.setFailCount(stats.fail());
        result.setTotalRequests(stats.totalRequests());
        result.setAvgLatencyMs(stats.avgLatencyMs());
        result.setMinLatencyMs(stats.minLatencyMs());
        result.setMaxLatencyMs(stats.maxLatencyMs());
        result.setP99LatencyMs(stats.p99LatencyMs());
        result.setEndedAt(LocalDateTime.now(clock));
        loadTestResultRepository.updateResult(result);
    }
}
