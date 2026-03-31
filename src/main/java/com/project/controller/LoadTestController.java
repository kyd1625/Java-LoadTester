package com.project.controller;

import com.project.controller.dto.CreateScenarioRequest;
import com.project.controller.dto.ScenarioCreateResponse;
import com.project.controller.dto.StartTestRequest;
import com.project.controller.dto.StartTestResponse;
import com.project.model.LoadTestFailLog;
import com.project.model.LoadTestResult;
import com.project.model.LoadTestScenario;
import com.project.service.LoadTestReadService;
import com.project.service.LoadTestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class LoadTestController {

    private final LoadTestService loadTestService;
    private final LoadTestReadService loadTestReadService;

    public LoadTestController(LoadTestService loadTestService, LoadTestReadService loadTestReadService) {
        this.loadTestService = loadTestService;
        this.loadTestReadService = loadTestReadService;
    }

    @PostMapping("/scenarios")
    public ResponseEntity<ScenarioCreateResponse> createScenario(@Valid @RequestBody CreateScenarioRequest request) {
        LoadTestScenario scenario = new LoadTestScenario();
        scenario.setName(request.name());
        scenario.setTargetUrl(request.targetUrl());
        scenario.setHttpMethod(request.httpMethod());
        scenario.setRequestParams(request.requestParams());
        scenario.setTargetTps(request.targetTps());
        scenario.setVirtualThreadCount(request.virtualThreadCount());
        scenario.setDurationSeconds(request.durationSeconds());

        Long scenarioId = loadTestService.createScenario(scenario);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ScenarioCreateResponse(scenarioId));
    }

    @PostMapping("/tests/start")
    public ResponseEntity<StartTestResponse> startLoadTest(@Valid @RequestBody StartTestRequest request) {
        Long resultId = loadTestService.startTestForScenarioId(request.scenarioId());
        return ResponseEntity.ok(new StartTestResponse(request.scenarioId(), resultId));
    }

    @GetMapping("/tests/{scenarioId}/result")
    public ResponseEntity<LoadTestResult> getResultByScenarioId(@PathVariable Long scenarioId) {
        LoadTestResult result = loadTestReadService.findResultByScenarioId(scenarioId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/tests/{resultId}/fail-logs")
    public ResponseEntity<List<LoadTestFailLog>> getFailLogsByResultId(@PathVariable Long resultId) {
        List<LoadTestFailLog> failLogs = loadTestReadService.findFailLogsByResultId(resultId);
        return ResponseEntity.ok(failLogs);
    }
}