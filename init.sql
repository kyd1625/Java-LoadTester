-- 1. loadtester 계정에게 DB 권한 부여
GRANT ALL PRIVILEGES ON load_tester_db.* TO 'loadtester'@'%';
FLUSH PRIVILEGES;

USE load_tester_db;

-- 2. 시나리오 테이블
CREATE TABLE load_test_scenario (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '시나리오 식별자',
    name VARCHAR(100) NOT NULL COMMENT '시나리오 명칭',
    target_url VARCHAR(255) NOT NULL COMMENT '테스트 대상 URL',
    http_method VARCHAR(10) NOT NULL COMMENT 'HTTP 메서드 (GET, POST 등)',
    request_params TEXT COMMENT '요청 파라미터 (JSON 형태)',
    target_tps INT NOT NULL COMMENT '초당 목표 요청 수 (Target TPS)',
    virtual_thread_count INT NOT NULL COMMENT '생성할 가상 쓰레드 수',
    duration_seconds INT NOT NULL COMMENT '테스트 지속 시간 (초)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시'
) COMMENT='부하 테스트 설정 시나리오';

-- 3. 테스트 결과 테이블
CREATE TABLE load_test_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '결과 식별자',
    scenario_id BIGINT NOT NULL COMMENT '연결된 시나리오 ID',
    total_requests INT NOT NULL COMMENT '총 발송 요청 수',
    success_count INT NOT NULL COMMENT '성공 요청 수',
    fail_count INT NOT NULL COMMENT '실패 요청 수',
    avg_latency_ms DOUBLE COMMENT '평균 응답 시간 (ms)',
    min_latency_ms DOUBLE COMMENT '최소 응답 시간 (ms)',
    max_latency_ms DOUBLE COMMENT '최대 응답 시간 (ms)',
    p99_latency_ms DOUBLE COMMENT '상위 1% 응답 시간 (ms)',
    started_at DATETIME COMMENT '테스트 시작 일시',
    ended_at DATETIME COMMENT '테스트 종료 일시',
    CONSTRAINT fk_scenario FOREIGN KEY (scenario_id) REFERENCES load_test_scenario(id) ON DELETE CASCADE
) COMMENT='부하 테스트 실행 결과 통계';

-- 4. 실패 로그 테이블
CREATE TABLE load_test_fail_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '로그 식별자',
    result_id BIGINT NOT NULL COMMENT '연결된 테스트 결과 ID',
    request_order BIGINT NOT NULL COMMENT '요청 발송 순번',
    error_msg TEXT COMMENT '에러 메시지 상세',
    request_time DATETIME COMMENT '실패 발생 시각',
    http_status INT COMMENT 'HTTP 상태 코드',
    CONSTRAINT fk_result FOREIGN KEY (result_id) REFERENCES load_test_result(id) ON DELETE CASCADE
) COMMENT='부하 테스트 중 발생한 개별 실패 로그';

CREATE INDEX idx_fail_log_result_id ON load_test_fail_log(result_id);