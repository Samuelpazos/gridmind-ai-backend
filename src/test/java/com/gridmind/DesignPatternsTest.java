package com.gridmind;

import com.gridmind.command.*;
import com.gridmind.domain.GridStatus;
import com.gridmind.domain.RiskLevel;
import com.gridmind.entity.AlertEntity;
import com.gridmind.entity.CommandLogEntity;
import com.gridmind.entity.CommandSnapshotEntity;
import com.gridmind.entity.TransformerEntity;
import com.gridmind.factory.IndustrialGridFactory;
import com.gridmind.factory.UrbanGridFactory;
import com.gridmind.repository.AlertRepository;
import com.gridmind.repository.CommandLogRepository;
import com.gridmind.repository.CommandSnapshotRepository;
import com.gridmind.repository.MaintenanceRecommendationRepository;
import com.gridmind.repository.TransformerRepository;
import com.gridmind.state.*;
import com.gridmind.strategy.EvaluationResult;
import com.gridmind.strategy.ThresholdStrategy;
import com.gridmind.strategy.WeightedAgeStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DesignPatternsTest {

    @Mock
    private TransformerRepository transformerRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private MaintenanceRecommendationRepository maintenanceRepository;

    @Mock
    private CommandLogRepository commandLogRepository;

    @Mock
    private CommandSnapshotRepository commandSnapshotRepository;

    @Test
    @DisplayName("State Pattern: Test transitions NORMAL -> WARNING -> CRITICAL -> FAILED -> NORMAL")
    void testStatePatternTransitions() {
        StateContext context = new StateContext(GridStatus.NORMAL);
        assertEquals(GridStatus.NORMAL, context.getCurrentState().getStatus());

        // Transition to WARNING (Probability >= 0.30)
        GridStatus s1 = context.evaluate(0.40, false);
        assertEquals(GridStatus.WARNING, s1);

        // Transition to CRITICAL (Probability >= 0.60)
        GridStatus s2 = context.evaluate(0.75, false);
        assertEquals(GridStatus.CRITICAL, s2);

        // Transition to FAILED (Probability > 0.92)
        GridStatus s3 = context.evaluate(0.95, false);
        assertEquals(GridStatus.FAILED, s3);

        // Cannot exit FAILED without completed maintenance
        GridStatus s4 = context.evaluate(0.05, false);
        assertEquals(GridStatus.FAILED, s4);

        // Exit FAILED back to NORMAL after maintenance completed
        GridStatus s5 = context.evaluate(0.05, true);
        assertEquals(GridStatus.NORMAL, s5);
    }

    @Test
    @DisplayName("State Pattern: Completed maintenance restores a critical transformer to NORMAL")
    void testMaintenanceRecoversCriticalState() {
        StateContext context = new StateContext(GridStatus.CRITICAL);

        assertEquals(GridStatus.NORMAL, context.evaluate(0.06, true));
    }

    @Test
    @DisplayName("Strategy Pattern: Test ThresholdStrategy vs WeightedAgeStrategy")
    void testStrategyPattern() {
        TransformerEntity mockTransformer = TransformerEntity.builder()
                .code("T-TEST")
                .name("Transformer Test")
                .ageYears(15)
                .build();

        Map<String, Double> readings = Map.of(
                "TEMPERATURE", 88.0,
                "VOLTAGE", 13.8,
                "CURRENT", 420.0,
                "VIBRATION", 5.0
        );

        ThresholdStrategy thresholdStrat = new ThresholdStrategy();
        EvaluationResult res1 = thresholdStrat.evaluate(mockTransformer, readings);
        assertNotNull(res1);
        assertEquals(0.99, res1.getFailureProbability(), 0.0001);
        assertEquals(RiskLevel.CRITICAL, res1.getRiskLevel());

        WeightedAgeStrategy ageStrat = new WeightedAgeStrategy();
        EvaluationResult res2 = ageStrat.evaluate(mockTransformer, readings);
        assertNotNull(res2);
        assertTrue(res2.getFailureProbability() > 0.50);
    }

    @Test
    @DisplayName("Abstract Factory Pattern: Urban vs Industrial components creation")
    void testAbstractFactoryPattern() {
        UrbanGridFactory urbanFactory = new UrbanGridFactory();
        IndustrialGridFactory industrialFactory = new IndustrialGridFactory();

        var urbanSub = urbanFactory.createSubstation("Sub Urbana", "Zona N");
        var urbanTrans = urbanFactory.createTransformer(urbanSub, "T-Urb", 750.0, 3);

        var indSub = industrialFactory.createSubstation("Sub Ind", "Zona S");
        var indTrans = industrialFactory.createTransformer(indSub, "T-Ind", 2000.0, 10);

        assertEquals("URBAN", urbanSub.getType().name());
        assertEquals(85.0, urbanTrans.getMaxTempC());

        assertEquals("INDUSTRIAL", indSub.getType().name());
        assertEquals(95.0, indTrans.getMaxTempC());
    }

    @Test
    @DisplayName("Command & Memento Pattern: Test SimulateFailureCommand and Undo")
    void testCommandAndMementoPattern() {
        TransformerEntity transformer = TransformerEntity.builder()
                .id("t-123")
                .status(GridStatus.NORMAL)
                .riskLevel(RiskLevel.LOW)
                .failureProbability(0.05)
                .priorityScore(5.0)
                .build();

        when(transformerRepository.findByIdAndArchivedAtIsNull("t-123"))
                .thenReturn(Optional.of(transformer));
        when(transformerRepository.save(any(TransformerEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        ArgumentCaptor<CommandLogEntity> logCaptor = ArgumentCaptor.forClass(CommandLogEntity.class);
        ArgumentCaptor<CommandSnapshotEntity> snapshotCaptor = ArgumentCaptor.forClass(CommandSnapshotEntity.class);
        when(commandLogRepository.save(any(CommandLogEntity.class))).thenAnswer(invocation -> {
            CommandLogEntity log = invocation.getArgument(0);
            if (log.getId() == null) log.setId("command-log-1");
            return log;
        });
        when(commandSnapshotRepository.save(any(CommandSnapshotEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        CommandInvoker invoker = new CommandInvoker(
                commandLogRepository, commandSnapshotRepository, new ObjectMapper());
        GridCommand command = new SimulateFailureCommand(transformerRepository, "t-123");

        // Execute Command
        invoker.executeCommand(command, "t-123", "param", "OPERATOR");
        assertEquals(GridStatus.FAILED, transformer.getStatus());
        assertEquals(RiskLevel.CRITICAL, transformer.getRiskLevel());
        verify(commandLogRepository).save(logCaptor.capture());
        verify(commandSnapshotRepository).save(snapshotCaptor.capture());
        CommandLogEntity savedLog = logCaptor.getValue();
        CommandSnapshotEntity snapshot = snapshotCaptor.getValue();
        assertNotNull(snapshot.getMementoState());
        assertSame(savedLog, snapshot.getCommandLog());
        when(commandSnapshotRepository
                .findFirstByCommandLog_ExecutedByAndConsumedAtIsNullOrderByCreatedAtDesc("OPERATOR"))
                .thenReturn(Optional.of(snapshot));
        doReturn(Optional.empty()).when(commandSnapshotRepository)
                .findFirstByCommandLog_ExecutedByAndConsumedAtIsNullOrderByCreatedAtDesc("OTHER_OPERATOR");
        assertFalse(invoker.undoLastCommand("OTHER_OPERATOR",
                log -> new SimulateFailureCommand(transformerRepository, log.getTargetId())));
        assertEquals(GridStatus.FAILED, transformer.getStatus());

        // Undo Command (Restores snapshot via Memento)
        boolean undone = invoker.undoLastCommand("OPERATOR",
                log -> new SimulateFailureCommand(transformerRepository, log.getTargetId()));
        assertTrue(undone);
        assertEquals(GridStatus.NORMAL, transformer.getStatus());
        assertEquals(RiskLevel.LOW, transformer.getRiskLevel());
    }

    @Test
    @DisplayName("Memento snapshots are persisted for maintenance commands")
    void persistsAndRestoresScheduledMaintenanceSnapshot() {
        TransformerEntity transformer = TransformerEntity.builder().id("t-456").build();
        when(transformerRepository.lockActiveById("t-456"))
                .thenReturn(Optional.of(transformer));
        when(maintenanceRepository.save(any())).thenAnswer(invocation -> {
            var recommendation = invocation.getArgument(0,
                    com.gridmind.entity.MaintenanceRecommendationEntity.class);
            recommendation.setId("maintenance-1");
            return recommendation;
        });
        when(commandLogRepository.save(any(CommandLogEntity.class))).thenAnswer(invocation -> {
            CommandLogEntity log = invocation.getArgument(0);
            if (log.getId() == null) log.setId("command-log-maintenance");
            return log;
        });
        when(commandSnapshotRepository.save(any(CommandSnapshotEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        CommandInvoker invoker = new CommandInvoker(
                commandLogRepository, commandSnapshotRepository, new ObjectMapper());
        invoker.executeCommand(new ScheduleMaintenanceCommand(
                transformerRepository, maintenanceRepository, "t-456", "Inspección"),
                "t-456", "{}", "OPERATOR");

        ArgumentCaptor<CommandSnapshotEntity> snapshotCaptor =
                ArgumentCaptor.forClass(CommandSnapshotEntity.class);
        verify(commandSnapshotRepository).save(snapshotCaptor.capture());
        assertTrue(snapshotCaptor.getValue().getMementoState().contains("maintenance-1"));
        when(commandSnapshotRepository
                .findFirstByCommandLog_ExecutedByAndConsumedAtIsNullOrderByCreatedAtDesc("OPERATOR"))
                .thenReturn(Optional.of(snapshotCaptor.getValue()));

        assertTrue(invoker.undoLastCommand("OPERATOR", log -> new ScheduleMaintenanceCommand(
                transformerRepository, maintenanceRepository, log.getTargetId(), "Inspección")));
        verify(maintenanceRepository).deleteById("maintenance-1");
    }

    @Test
    @DisplayName("Memento snapshots are persisted for alert acknowledgment commands")
    void persistsAndRestoresAcknowledgmentSnapshot() {
        AlertEntity alert = AlertEntity.builder()
                .id("alert-456")
                .severity(GridStatus.WARNING)
                .acknowledged(false)
                .build();
        when(alertRepository.findById("alert-456")).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(AlertEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(commandLogRepository.save(any(CommandLogEntity.class))).thenAnswer(invocation -> {
            CommandLogEntity log = invocation.getArgument(0);
            if (log.getId() == null) log.setId("command-log-alert");
            return log;
        });
        when(commandSnapshotRepository.save(any(CommandSnapshotEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        CommandInvoker invoker = new CommandInvoker(
                commandLogRepository, commandSnapshotRepository, new ObjectMapper());
        invoker.executeCommand(new AcknowledgeAlertCommand(alertRepository, "alert-456", "OPERATOR"),
                "alert-456", "{}", "OPERATOR");
        assertTrue(alert.getAcknowledged());

        ArgumentCaptor<CommandSnapshotEntity> snapshotCaptor =
                ArgumentCaptor.forClass(CommandSnapshotEntity.class);
        verify(commandSnapshotRepository).save(snapshotCaptor.capture());
        assertTrue(snapshotCaptor.getValue().getMementoState().contains("\"acknowledged\":false"));
        when(commandSnapshotRepository
                .findFirstByCommandLog_ExecutedByAndConsumedAtIsNullOrderByCreatedAtDesc("OPERATOR"))
                .thenReturn(Optional.of(snapshotCaptor.getValue()));

        assertTrue(invoker.undoLastCommand("OPERATOR",
                log -> new AcknowledgeAlertCommand(alertRepository, log.getTargetId(), "OPERATOR")));
        assertFalse(alert.getAcknowledged());
    }

    @Test
    @DisplayName("Command Pattern: Test AcknowledgeAlertCommand")
    void testAcknowledgeAlertCommand() {
        AlertEntity alert = AlertEntity.builder()
                .id("alt-123")
                .severity(GridStatus.CRITICAL)
                .message("Test Alert")
                .acknowledged(false)
                .build();

        when(alertRepository.findById("alt-123")).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(AlertEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        AcknowledgeAlertCommand cmd = new AcknowledgeAlertCommand(alertRepository, "alt-123", "ADMIN_USER");
        cmd.execute();

        assertTrue(alert.getAcknowledged());
        assertEquals("ADMIN_USER", alert.getAcknowledgedBy());

        cmd.undo();
        assertFalse(alert.getAcknowledged());
    }
}
