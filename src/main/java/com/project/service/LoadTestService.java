package com.project.service;

import com.project.model.LoadTestScenario;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class LoadTestService {

    private final WebClient.Builder webClientBuilder;

    public LoadTestService(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
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
            // FailLog 구현
        }
        
    }

}
