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

    public void startTestEngine(LoadTestScenario scenario) {

        // 1. 시나리오 저장 (MyBatis가 scenario 객체에 id를 채워줌)
        loadTestScenarioRepository.insertScenario(scenario);
        Long scenarioId = scenario.getId(); // 시나리오 ID 획득

        // 1. 빈 껍데기(초기 결과) 생성 및 저장
        LoadTestResult result = new LoadTestResult();
        result.setScenarioId(scenarioId);
        result.setStartedAt(LocalDateTime.now());

        // DB에 넣으면 MyBatis가 result 객체 안에 자동으로 생성된 ID를 채워줍니다.
        loadTestResultRepository.insertResult(result);
        Long resultId = result.getId(); // 드디어 생긴 ID!

        // 2. 실제 테스트 실행 (이 ID를 들고 갑니다)
        executeLoadTest(scenario, resultId);

        // 3. 테스트 완료 후 최종 업데이트
        result.setEndedAt(LocalDateTime.now());
        // result.setTotalSuccess(count...); // 계산된 결과 세팅
        loadTestResultRepository.updateResult(result);
    }

    /**
     * Virtual Threads execute
     * @param scenario
     **/
    public void executeLoadTest(LoadTestScenario scenario, Long resultId) {

        AtomicLong globalOrder = new AtomicLong(0); // 여러 쓰레드 접근시 데이터 유실 발생 가능성 으로 CAS(Compare-And-Swap) 사용

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            for(int i = 0; i < scenario.getVirtualThreadCount(); i++) {
                executor.submit(() -> {
                    long currentOrder = globalOrder.incrementAndGet();
                   callRequest(scenario, resultId, currentOrder);
                });
            }



        }

    }

    /**
     * api Request
     * @param scenario
     */
    public void callRequest(LoadTestScenario scenario, long resultId, long currentOrder) {
        
        long startTime = System.currentTimeMillis(); 
        
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

            loadTestFailLogRepository.insertFailLog(failLog); // DB 저장 실행
        }
        
    }

}
