package com.project.service;

import com.project.model.LoadTestResult;
import com.project.model.LoadTestScenario;
import com.project.repository.LoadTestResultRepository;
import com.project.repository.LoadTestScenarioRepository;
import com.project.service.dto.TestStats;
import com.project.service.runner.LoadTestRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoadTestServiceTest {

    @Mock
    private LoadTestRunner loadTestRunner;

    @Mock
    private LoadTestResultRepository loadTestResultRepository;

    @Mock
    private LoadTestScenarioRepository loadTestScenarioRepository;

    private LoadTestService loadTestService;
    private Clock fixedClock;
    private LocalDateTime fixedNow;

    @BeforeEach
    void setUp() {
        Instant fixedInstant = Instant.parse("2026-02-17T10:15:30Z");
        fixedClock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));
        fixedNow = LocalDateTime.now(fixedClock);
        loadTestService = new LoadTestService(
                loadTestRunner,
                loadTestResultRepository,
                loadTestScenarioRepository,
                fixedClock
        );
    }

    @Test
    @DisplayName("startTestEngine: 정상 흐름에서 scenario insert -> result insert -> runner -> update 순서로 호출한다")
    void startTestEngine_happyPath_callOrderIsCorrect() {
        // Arrange
        LoadTestScenario scenario = sampleScenario();
        TestStats stats = sampleStats();
        stubScenarioAndResultIds(10L, 20L);
        when(loadTestRunner.run(scenario, 20L)).thenReturn(stats);

        // Act
        loadTestService.startTestEngine(scenario);

        // Assert
        InOrder inOrder = inOrder(loadTestScenarioRepository, loadTestResultRepository, loadTestRunner);
        inOrder.verify(loadTestScenarioRepository).insertScenario(scenario);
        inOrder.verify(loadTestResultRepository).insertResult(any(LoadTestResult.class));
        inOrder.verify(loadTestRunner).run(scenario, 20L);
        inOrder.verify(loadTestResultRepository).updateResult(any(LoadTestResult.class));
    }

    @Test
    @DisplayName("startTestEngine: scenario insert로 생성된 ID를 result.scenarioId에 반영한다")
    void startTestEngine_generatedScenarioId_isUsedForResultScenarioId() {
        // Arrange
        LoadTestScenario scenario = sampleScenario();
        TestStats stats = sampleStats();
        AtomicReference<LoadTestResult> insertedSnapshot = new AtomicReference<>();

        doAnswer(invocation -> {
            LoadTestScenario arg = invocation.getArgument(0);
            arg.setId(101L);
            return null;
        }).when(loadTestScenarioRepository).insertScenario(scenario);

        doAnswer(invocation -> {
            LoadTestResult arg = invocation.getArgument(0);
            insertedSnapshot.set(copyResult(arg));
            arg.setId(202L);
            return null;
        }).when(loadTestResultRepository).insertResult(any(LoadTestResult.class));

        when(loadTestRunner.run(scenario, 202L)).thenReturn(stats);

        // Act
        loadTestService.startTestEngine(scenario);

        // Assert
        assertThat(insertedSnapshot.get()).isNotNull();
        assertThat(insertedSnapshot.get().getScenarioId()).isEqualTo(101L);
    }

    @Test
    @DisplayName("startTestEngine: runner 결과(TestStats)를 updateResult에 그대로 반영한다")
    void startTestEngine_runnerStats_areMappedToUpdatedResult() {
        // Arrange
        LoadTestScenario scenario = sampleScenario();
        TestStats stats = new TestStats(70, 30, 100, 12.5, 3.0, 44.0, 40.0);
        stubScenarioAndResultIds(11L, 22L);
        when(loadTestRunner.run(scenario, 22L)).thenReturn(stats);

        ArgumentCaptor<LoadTestResult> updateCaptor = ArgumentCaptor.forClass(LoadTestResult.class);

        // Act
        loadTestService.startTestEngine(scenario);

        // Assert
        verify(loadTestResultRepository).updateResult(updateCaptor.capture());
        LoadTestResult updated = updateCaptor.getValue();

        assertThat(updated.getId()).isEqualTo(22L);
        assertThat(updated.getSuccessCount()).isEqualTo(70);
        assertThat(updated.getFailCount()).isEqualTo(30);
        assertThat(updated.getTotalRequests()).isEqualTo(100);
        assertThat(updated.getAvgLatencyMs()).isEqualTo(12.5);
        assertThat(updated.getMinLatencyMs()).isEqualTo(3.0);
        assertThat(updated.getMaxLatencyMs()).isEqualTo(44.0);
        assertThat(updated.getP99LatencyMs()).isEqualTo(40.0);
    }

    @Test
    @DisplayName("startTestEngine: startedAt/endedAt은 주입된 Clock 기준으로 설정된다")
    void startTestEngine_timeFields_areSetFromInjectedClock() {
        // Arrange
        LoadTestScenario scenario = sampleScenario();
        TestStats stats = sampleStats();
        stubScenarioAndResultIds(33L, 44L);
        when(loadTestRunner.run(scenario, 44L)).thenReturn(stats);

        ArgumentCaptor<LoadTestResult> insertCaptor = ArgumentCaptor.forClass(LoadTestResult.class);
        ArgumentCaptor<LoadTestResult> updateCaptor = ArgumentCaptor.forClass(LoadTestResult.class);

        // Act
        loadTestService.startTestEngine(scenario);

        // Assert
        verify(loadTestResultRepository).insertResult(insertCaptor.capture());
        verify(loadTestResultRepository).updateResult(updateCaptor.capture());

        assertThat(insertCaptor.getValue().getStartedAt()).isEqualTo(fixedNow);
        assertThat(updateCaptor.getValue().getEndedAt()).isEqualTo(fixedNow);
    }

    @Test
    @DisplayName("startTestEngine: 통계가 0이어도 updateResult가 정상 호출된다")
    void startTestEngine_zeroStats_updatesWithZeros() {
        // Arrange
        LoadTestScenario scenario = sampleScenario();
        TestStats zeroStats = new TestStats(0, 0, 0, 0.0, 0.0, 0.0, 0.0);
        stubScenarioAndResultIds(1L, 2L);
        when(loadTestRunner.run(scenario, 2L)).thenReturn(zeroStats);

        ArgumentCaptor<LoadTestResult> updateCaptor = ArgumentCaptor.forClass(LoadTestResult.class);

        // Act
        loadTestService.startTestEngine(scenario);

        // Assert
        verify(loadTestResultRepository).updateResult(updateCaptor.capture());
        LoadTestResult updated = updateCaptor.getValue();
        assertThat(updated.getTotalRequests()).isZero();
        assertThat(updated.getSuccessCount()).isZero();
        assertThat(updated.getFailCount()).isZero();
        assertThat(updated.getAvgLatencyMs()).isZero();
        assertThat(updated.getP99LatencyMs()).isZero();
    }

    @Test
    @DisplayName("startTestEngine: scenario insert 실패 시 예외를 전파하고 나머지 의존성은 호출하지 않는다")
    void startTestEngine_whenScenarioInsertFails_propagatesAndStops() {
        // Arrange
        LoadTestScenario scenario = sampleScenario();
        RuntimeException expected = new RuntimeException("scenario insert fail");
        doThrow(expected).when(loadTestScenarioRepository).insertScenario(scenario);

        // Act
        RuntimeException actual = assertThrows(RuntimeException.class, () -> loadTestService.startTestEngine(scenario));

        // Assert
        assertThat(actual).isSameAs(expected);
        verifyNoInteractions(loadTestResultRepository, loadTestRunner);
    }

    @Test
    @DisplayName("startTestEngine: result insert 실패 시 예외를 전파하고 runner/update를 호출하지 않는다")
    void startTestEngine_whenResultInsertFails_propagatesAndSkipsRunnerAndUpdate() {
        // Arrange
        LoadTestScenario scenario = sampleScenario();
        doAnswer(invocation -> {
            LoadTestScenario arg = invocation.getArgument(0);
            arg.setId(555L);
            return null;
        }).when(loadTestScenarioRepository).insertScenario(scenario);
        doThrow(new RuntimeException("result insert fail"))
                .when(loadTestResultRepository).insertResult(any(LoadTestResult.class));

        // Act
        assertThrows(RuntimeException.class, () -> loadTestService.startTestEngine(scenario));

        // Assert
        verify(loadTestResultRepository, never()).updateResult(any(LoadTestResult.class));
        verifyNoInteractions(loadTestRunner);
    }

    @Test
    @DisplayName("startTestEngine: runner 예외 시 예외를 전파하고 updateResult는 호출하지 않는다")
    void startTestEngine_whenRunnerFails_propagatesAndSkipsUpdate() {
        // Arrange
        LoadTestScenario scenario = sampleScenario();
        stubScenarioAndResultIds(77L, 88L);
        when(loadTestRunner.run(scenario, 88L)).thenThrow(new RuntimeException("runner failed"));

        // Act
        assertThrows(RuntimeException.class, () -> loadTestService.startTestEngine(scenario));

        // Assert
        verify(loadTestResultRepository, never()).updateResult(any(LoadTestResult.class));
    }

    @Test
    @DisplayName("startTestEngine: updateResult 실패 시 예외를 전파한다")
    void startTestEngine_whenUpdateFails_propagatesException() {
        // Arrange
        LoadTestScenario scenario = sampleScenario();
        stubScenarioAndResultIds(9L, 19L);
        when(loadTestRunner.run(scenario, 19L)).thenReturn(sampleStats());
        doThrow(new RuntimeException("update fail"))
                .when(loadTestResultRepository).updateResult(any(LoadTestResult.class));

        // Act
        RuntimeException exception = assertThrows(RuntimeException.class, () -> loadTestService.startTestEngine(scenario));

        // Assert
        assertThat(exception).hasMessage("update fail");
        verify(loadTestRunner).run(scenario, 19L);
    }

    private void stubScenarioAndResultIds(long scenarioId, long resultId) {
        doAnswer(invocation -> {
            LoadTestScenario arg = invocation.getArgument(0);
            arg.setId(scenarioId);
            return null;
        }).when(loadTestScenarioRepository).insertScenario(any(LoadTestScenario.class));

        doAnswer(invocation -> {
            LoadTestResult arg = invocation.getArgument(0);
            arg.setId(resultId);
            return null;
        }).when(loadTestResultRepository).insertResult(any(LoadTestResult.class));
    }

    private LoadTestScenario sampleScenario() {
        LoadTestScenario scenario = new LoadTestScenario();
        scenario.setName("unit-test-scenario");
        scenario.setTargetUrl("http://localhost:8080/test");
        scenario.setHttpMethod("GET");
        scenario.setRequestParams("{}");
        scenario.setTargetTps(100);
        scenario.setVirtualThreadCount(10);
        scenario.setDurationSeconds(5);
        return scenario;
    }

    private TestStats sampleStats() {
        return new TestStats(90, 10, 100, 11.0, 2.0, 41.0, 39.0);
    }

    private LoadTestResult copyResult(LoadTestResult source) {
        LoadTestResult copied = new LoadTestResult();
        copied.setId(source.getId());
        copied.setScenarioId(source.getScenarioId());
        copied.setTotalRequests(source.getTotalRequests());
        copied.setSuccessCount(source.getSuccessCount());
        copied.setFailCount(source.getFailCount());
        copied.setAvgLatencyMs(source.getAvgLatencyMs());
        copied.setMinLatencyMs(source.getMinLatencyMs());
        copied.setMaxLatencyMs(source.getMaxLatencyMs());
        copied.setP99LatencyMs(source.getP99LatencyMs());
        copied.setStartedAt(source.getStartedAt());
        copied.setEndedAt(source.getEndedAt());
        return copied;
    }
}
