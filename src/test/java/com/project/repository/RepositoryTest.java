package com.project.repository;

import com.project.model.LoadTestFailLog;
import com.project.model.LoadTestResult;
import com.project.model.LoadTestScenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional // 테스트가 끝나면 데이터를 자동으로 롤백(삭제)하여 DB를 깨끗하게 유지
class RepositoryTest {

    @Autowired
    LoadTestScenarioRepository scenarioRepository;
    @Autowired
    LoadTestResultRepository resultRepository;
    @Autowired
    LoadTestFailLogRepository failLogRepository;

    @Test
    @DisplayName("시나리오_저장_및_한글_조회_테스트")
    void scenarioInsertAndSelectTest() {
        // 1. Given: 시나리오 데이터 생성
        LoadTestScenario scenario = new LoadTestScenario();
        scenario.setName("메인 페이지 부하 테스트 (한글 확인)"); // 한글 테스트
        scenario.setTargetUrl("http://localhost:8080/api/test");
        scenario.setHttpMethod("POST");
        scenario.setRequestParams("{\"key\":\"value\"}");
        scenario.setTargetTps(100);
        scenario.setVirtualThreadCount(50);
        scenario.setDurationSeconds(60);

        // 2. When: 저장 실행
        scenarioRepository.insertScenario(scenario);

        // 3. Then: 검증
        // ID가 자동으로 생성되었는지 확인 (useGeneratedKeys="true" 덕분)
        assertThat(scenario.getId()).isNotNull();
        System.out.println("생성된 시나리오 ID: " + scenario.getId());

        // DB에서 다시 조회해서 한글이 깨지지 않았는지 확인
        LoadTestScenario savedScenario = scenarioRepository.selectScenarioById(scenario.getId());
        assertThat(savedScenario.getName()).isEqualTo("메인 페이지 부하 테스트 (한글 확인)");

        System.out.println("조회된 시나리오 이름: " + savedScenario.getName());
    }

    @Test
    @DisplayName("결과_및_실패로그_저장_테스트")
    void resultAndFailLogTest() {
        // --- 1. 부모 데이터(Scenario) 먼저 저장 ---
        LoadTestScenario scenario = new LoadTestScenario();
        scenario.setName("실패 로그 테스트용 시나리오");
        scenario.setTargetUrl("http://test.com");
        scenario.setHttpMethod("GET");
        scenario.setTargetTps(10);
        scenario.setVirtualThreadCount(5);
        scenario.setDurationSeconds(10);
        scenarioRepository.insertScenario(scenario);

        // --- 2. 결과(Result) 저장 ---
        LoadTestResult result = new LoadTestResult();
        result.setScenarioId(scenario.getId()); // FK 연결
        result.setTotalRequests(1000);
        result.setSuccessCount(900);
        result.setFailCount(100);
        result.setAvgLatencyMs(120.5);
        result.setMinLatencyMs(50.0);
        result.setMaxLatencyMs(2000.0);
        result.setP99LatencyMs(1500.0);
        result.setStartedAt(LocalDateTime.now().toString());
        result.setEndedAt(LocalDateTime.now().plusMinutes(1).toString());

        resultRepository.insertResult(result);
        Long generatedId = result.getId();
        assertThat(generatedId).isNotNull();

        // --- 3. 실패 로그(FailLog) 저장 ---
        LoadTestFailLog failLog = new LoadTestFailLog();
        failLog.setResultId(generatedId); // FK 연결
        failLog.setRequestOrder(15L);
        failLog.setErrorMsg("Connection Refused 에러 발생");
        failLog.setRequestTime(LocalDateTime.now().toString());
        failLog.setHttpStatus(500);

        failLogRepository.insertFailLog(failLog);

        // --- 4. 검증 ---
        List<LoadTestFailLog> logs = failLogRepository.selectFailLogByResultId(result.getId());
        assertThat(logs).isNotEmpty();
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getErrorMsg()).contains("Connection Refused");

        System.out.println("저장된 에러 메시지: " + logs.get(0).getErrorMsg());
    }
}