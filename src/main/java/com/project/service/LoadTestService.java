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
        loadTestScenarioRepository.insertScenario(scenario);
        executeScenario(scenario);
    }

    public Long createScenario(LoadTestScenario scenario) {
        loadTestScenarioRepository.insertScenario(scenario);
        return scenario.getId();
    }

    public Long startTestForScenarioId(Long scenarioId) {
        LoadTestScenario scenario = loadTestScenarioRepository.selectScenarioById(scenarioId);
        if (scenario == null) {
            throw new IllegalArgumentException("시나리오를 찾을 수 없습니다. scenarioId=" + scenarioId);
        }

        return executeScenario(scenario);
    }

    private Long executeScenario(LoadTestScenario scenario) {
        LoadTestResult result = new LoadTestResult();
        result.setScenarioId(scenario.getId());
        result.setStartedAt(LocalDateTime.now(clock));
        loadTestResultRepository.insertResult(result);

        TestStats stats = loadTestRunner.run(scenario, result.getId());

        result.setSuccessCount(stats.success());
        result.setFailCount(stats.fail());
        result.setTotalRequests(stats.totalRequests());
        result.setAvgLatencyMs(stats.avgLatencyMs());
        result.setMinLatencyMs(stats.minLatencyMs());
        result.setMaxLatencyMs(stats.maxLatencyMs());
        result.setP99LatencyMs(stats.p99LatencyMs());
        result.setEndedAt(LocalDateTime.now(clock));
        loadTestResultRepository.updateResult(result);
        return result.getId();
    }
}