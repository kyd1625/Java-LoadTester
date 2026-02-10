package com.project.service;

import com.project.model.LoadTestFailLog;
import com.project.model.LoadTestResult;
import com.project.model.LoadTestScenario;
import com.project.repository.LoadTestFailLogRepository;
import com.project.repository.LoadTestResultRepository;
import com.project.repository.LoadTestScenarioRepository;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class LoadTestService {

    private final WebClient.Builder webClientBuilder;
    private final LoadTestFailLogRepository loadTestFailLogRepository;
    private final LoadTestResultRepository loadTestResultRepository;
    private final LoadTestScenarioRepository loadTestScenarioRepository;

    public LoadTestService(WebClient.Builder webClientBuilder, LoadTestFailLogRepository loadTestFailLogRepository, LoadTestResultRepository loadTestResultRepository, LoadTestScenarioRepository loadTestScenarioRepository) {
        this.webClientBuilder = webClientBuilder;
        this.loadTestFailLogRepository = loadTestFailLogRepository;
        this.loadTestResultRepository = loadTestResultRepository;
        this.loadTestScenarioRepository = loadTestScenarioRepository;
    }

    public record TestStats(int success, int fail) {}

    public void startTestEngine(LoadTestScenario scenario) {

        // 1. 시나리오ID를 가져오기 위해 시나리오 저장 (Id값 자동 생성)
        loadTestScenarioRepository.insertScenario(scenario);
        Long scenarioId = scenario.getId(); 

        // 1. ResultID를 가져오기 위해 데이터 생성 및 저장 (Id값 자동 생성)
        LoadTestResult result = new LoadTestResult();
        result.setScenarioId(scenarioId);
        result.setStartedAt(LocalDateTime.now());
        loadTestResultRepository.insertResult(result);
        Long resultId = result.getId();

        // 2. 테스트 실행
        TestStats stats = executeLoadTest(scenario, result.getId());

        // 3. 테스트 완료 후 최종 업데이트
        result.setEndedAt(LocalDateTime.now());
        // result.setTotalSuccess(count...); // 계산된 결과 세팅
        loadTestResultRepository.updateResult(result);
    }

    /**
     * Virtual Threads execute
     * @param scenario
     * @return TestStats (성공, 실패 횟수)
     **/
    public TestStats executeLoadTest(LoadTestScenario scenario, Long resultId) {

        AtomicLong globalOrder = new AtomicLong(0); // 여러 쓰레드 접근시 데이터 유실 발생 가능성 으로 CAS(Compare-And-Swap) 사용
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // 종료시간 = 현재시간 + 지속시간
        long endTimeMillis = System.currentTimeMillis() + (scenario.getDurationSeconds() * 1000L);


        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            for(int i = 0; i < scenario.getVirtualThreadCount(); i++) {
                executor.submit(() -> {

                    while (System.currentTimeMillis() < endTimeMillis) {
                        long currentOrder = globalOrder.incrementAndGet();
                        boolean isSuccess = callRequest(scenario, resultId, currentOrder);

                        if (isSuccess) {
                            successCount.incrementAndGet(); // 성공시 +1
                        } else {
                            failCount.incrementAndGet(); // 실패시 +1
                        }
                    }


                });


            }

        }
        return new TestStats(successCount.get(), failCount.get());

    }

    /**
     * api Request
     * @param scenario
     */
    public boolean callRequest(LoadTestScenario scenario, long resultId, long currentOrder) {

        long startTime = System.currentTimeMillis();
        boolean isSuccess; // Request 별 성공/실패 유무

        try {
            webClientBuilder.build()
                    .method(HttpMethod.valueOf(scenario.getHttpMethod()))
                    .uri(scenario.getTargetUrl())
                    .bodyValue(scenario.getRequestParams())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response ->
                            Mono.error(new WebClientResponseException(
                                    response.statusCode().value(), // 상태 코드 숫자 (예: 404)
                                    "HTTP Error",
                                    null, null, null))
                    )
                    .bodyToMono(String.class)
                    .block();

            isSuccess = true; // 성공
            long endTime = System.currentTimeMillis();
        } catch (Exception e) {
            // 실패 시 DB에 로그 저장
            LoadTestFailLog failLog = new LoadTestFailLog();
            failLog.setResultId(resultId);             // 부모 결과 ID
            failLog.setRequestOrder(currentOrder);
            failLog.setErrorMsg(e.getMessage());
            failLog.setRequestTime(LocalDateTime.now());
            if (e instanceof WebClientResponseException ex) {
                // 서버가 응답은 했으나 에러인 경우 (404, 500 등)
                failLog.setHttpStatus(ex.getStatusCode().value());
            } else {
                // 서버 연결 자체가 안 된 경우 (Timeout, Connection Refused 등)
                failLog.setHttpStatus(0); // 0이나 특정 약속된 코드로 저장
            }

            isSuccess = false; // 실패
            loadTestFailLogRepository.insertFailLog(failLog); // DB 저장 실행
        }


        return isSuccess;
    }

}
