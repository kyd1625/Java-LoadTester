package com.project.repository;

import com.project.model.LoadTestResult;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface LoadTestResultRepository {
    void insertResult(LoadTestResult result);
    void updateResult(LoadTestResult result);
    LoadTestResult selectResultByScenarioId(Long scenarioId);
    void deleteResultByScenarioId(Long scenarioId);
}
