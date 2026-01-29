package com.project.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoadTestResult {
    private Long id;
    private Long scenarioId;       // 연결된 시나리오 ID (외래키 역할)

    // 결과 통계 지표
    private int totalRequests;     // 총 발송한 요청 수
    private int successCount;      // 성공한 요청 수 (HTTP 200 등)
    private int failCount;         // 실패한 요청 수

    // 성능 측정 지표 (ms 단위)
    private double avgLatencyMs;   // 평균 응답 시간
    private double minLatencyMs;   // 최소 응답 시간
    private double maxLatencyMs;   // 최대 응답 시간
    private double p99LatencyMs;   // 상위 1% 응답 시간 (꼬리 지연 확인용)

    // 시간 정보
    private String startedAt;      // 테스트 시작 시각
    private String endedAt;        // 테스트 종료 시각
}