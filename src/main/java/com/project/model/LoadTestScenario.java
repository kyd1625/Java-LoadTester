package com.project.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoadTestScenario {
    private Long id;
    private String name;           // 시나리오 명

    // 요청 관련 설정
    private String targetUrl;      // 대상 API URL
    private String httpMethod;     // GET, POST, PUT, DELETE 등
    private String requestParams;  // JSON 형태의 파라미터나 쿼리 스트링

    // 부하 제어 관련 설정
    private int targetTps;         // 목표 TPS (초당 생성할 요청 수)
    private int virtualThreadCount;// 사용할 가상 쓰레드(Worker) 개수
    private int durationSeconds;   // 테스트 지속 시간 (초)

    private LocalDateTime createdAt;
}
