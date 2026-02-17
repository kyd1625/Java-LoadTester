package com.project.service.client;

import com.project.model.LoadTestFailLog;
import com.project.model.LoadTestScenario;
import com.project.repository.LoadTestFailLogRepository;
import com.project.service.dto.RequestResult;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Component
public class LoadTestRequestExecutor {

    private final WebClient.Builder webClientBuilder;
    private final LoadTestFailLogRepository loadTestFailLogRepository;

    public LoadTestRequestExecutor(WebClient.Builder webClientBuilder, LoadTestFailLogRepository loadTestFailLogRepository) {
        this.webClientBuilder = webClientBuilder;
        this.loadTestFailLogRepository = loadTestFailLogRepository;
    }

    public RequestResult execute(LoadTestScenario scenario, long resultId, long currentOrder) {
        long startTime = System.currentTimeMillis();
        boolean isSuccess;

        try {
            // 시나리오 설정(메서드/URL/파라미터)으로 실제 HTTP 요청 실행
            webClientBuilder.build()
                    .method(HttpMethod.valueOf(scenario.getHttpMethod()))
                    .uri(scenario.getTargetUrl())
                    .bodyValue(scenario.getRequestParams())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response ->
                            Mono.error(new WebClientResponseException(
                                    response.statusCode().value(),
                                    "HTTP Error",
                                    null, null, null))
                    )
                    .bodyToMono(String.class)
                    .block();
            isSuccess = true;
        } catch (Exception e) {
            // 실패 요청은 상세 로그(resultId, 순번, 에러메시지, 상태코드)로 저장
            LoadTestFailLog failLog = new LoadTestFailLog();
            failLog.setResultId(resultId);
            failLog.setRequestOrder(currentOrder);
            failLog.setErrorMsg(e.getMessage());
            failLog.setRequestTime(LocalDateTime.now());

            if (e instanceof WebClientResponseException ex) {
                failLog.setHttpStatus(ex.getStatusCode().value());
            } else {
                failLog.setHttpStatus(0);
            }

            loadTestFailLogRepository.insertFailLog(failLog);
            isSuccess = false;
        }

        // 요청 지연시간(ms) 측정값과 성공 여부를 함께 반환
        long latencyMs = System.currentTimeMillis() - startTime;
        return new RequestResult(isSuccess, latencyMs);
    }
}
