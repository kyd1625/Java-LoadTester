package com.project.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoadTestFailLog {
    private Long id;
    private Long resultId;        // 테스트 결과 ID
    private long requestOrder;    // [추가] 몇 번째 호출이었는지 (순번)

    private String errorMsg;      // 에러 메시지
    private LocalDateTime requestTime;   // 실패 시간
    private int httpStatus;       // HTTP 상태 코드
}