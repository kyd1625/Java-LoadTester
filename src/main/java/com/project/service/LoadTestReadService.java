package com.project.service;

import com.project.model.LoadTestFailLog;
import com.project.model.LoadTestResult;
import com.project.repository.LoadTestFailLogRepository;
import com.project.repository.LoadTestResultRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LoadTestReadService {

    private final LoadTestResultRepository loadTestResultRepository;
    private final LoadTestFailLogRepository loadTestFailLogRepository;

    public LoadTestReadService(
            LoadTestResultRepository loadTestResultRepository,
            LoadTestFailLogRepository loadTestFailLogRepository
    ) {
        this.loadTestResultRepository = loadTestResultRepository;
        this.loadTestFailLogRepository = loadTestFailLogRepository;
    }

    public LoadTestResult findResultByScenarioId(Long scenarioId) {
        LoadTestResult result = loadTestResultRepository.selectResultByScenarioId(scenarioId);
        if (result == null) {
            throw new IllegalArgumentException("테스트 결과를 찾을 수 없습니다. scenarioId=" + scenarioId);
        }
        return result;
    }

    public List<LoadTestFailLog> findFailLogsByResultId(Long resultId) {
        return loadTestFailLogRepository.selectFailLogByResultId(resultId);
    }
}