-- 1. loadtester 계정에게 DB 권한 부여 (이미 생성된 유저에게 권한 할당)
GRANT ALL PRIVILEGES ON load_tester_db.* TO 'loadtester'@'%';
FLUSH PRIVILEGES;

USE load_tester_db;

-- 2. 시나리오 테이블
CREATE TABLE load_test_scenario (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    target_url VARCHAR(255) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    request_params TEXT,
    target_tps INT NOT NULL,
    virtual_thread_count INT NOT NULL,
    duration_seconds INT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 3. 테스트 결과 테이블
CREATE TABLE load_test_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scenario_id BIGINT NOT NULL,
    total_requests INT NOT NULL,
    success_count INT NOT NULL,
    fail_count INT NOT NULL,
    avg_latency_ms DOUBLE,
    min_latency_ms DOUBLE,
    max_latency_ms DOUBLE,
    p99_latency_ms DOUBLE,
    started_at DATETIME,
    ended_at DATETIME,
    CONSTRAINT fk_scenario FOREIGN KEY (scenario_id) REFERENCES load_test_scenario(id) ON DELETE CASCADE
);

-- 4. 실패 로그 테이블
CREATE TABLE load_test_fail_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    result_id BIGINT NOT NULL,
    request_order BIGINT NOT NULL,
    error_msg TEXT,
    request_time DATETIME,
    http_status INT,
    CONSTRAINT fk_result FOREIGN KEY (result_id) REFERENCES load_test_result(id) ON DELETE CASCADE
);

CREATE INDEX idx_fail_log_result_id ON load_test_fail_log(result_id);