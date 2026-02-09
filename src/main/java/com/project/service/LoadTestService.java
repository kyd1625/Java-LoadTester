package com.project.service;

import com.project.model.LoadTestFailLog;
import com.project.model.LoadTestScenario;
import com.project.repository.LoadTestFailLogRepository;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class LoadTestService {

    private final WebClient.Builder webClientBuilder;
    private final LoadTestFailLogRepository loadTestFailLogRepository;

    public LoadTestService(WebClient.Builder webClientBuilder, LoadTestFailLogRepository loadTestFailLogRepository) {
        this.webClientBuilder = webClientBuilder;
        this.loadTestFailLogRepository = loadTestFailLogRepository;
    }

    /**
     * Virtual Threads execute
     * @param scenario
     **/
    public void executeLoadTest(LoadTestScenario scenario) {

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            for(int i = 0; i < scenario.getVirtualThreadCount(); i++) {
                executor.submit(() -> {
                   callRequest(scenario);
                });
            }

        }

    }

    /**
     * api Request
     * @param scenario
     */
    public void callRequest(LoadTestScenario scenario) {
        
        long startTime = System.currentTimeMillis(); 
        
        try {
            webClientBuilder.build()
                    .method(HttpMethod.valueOf(scenario.getHttpMethod()))
                    .uri(scenario.getTargetUrl())
                    .bodyValue(scenario.getRequestParams())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            long endTime = System.currentTimeMillis();
        } catch (Exception e) {
            // 실패 시 DB에 로그 저장
            LoadTestFailLog failLog = new LoadTestFailLog();
            failLog.setResultId(resultId);             // 부모 결과 ID
            failLog.setRequestOrder(order);           // 몇 번째 요청이었는지
            failLog.setErrorMsg(e.getMessage());      // 에러 메시지 (예: Connection Refused)
            failLog.setRequestTime(LocalDateTime.now()); // 발생 시각
            failLog.setHttpStatus(500);               // 기본값 500 (필요시 상세 분류 가능)

            loadTestFailLogRepository.insertFailLog(failLog); // DB 저장 실행
        }
        
    }

}
