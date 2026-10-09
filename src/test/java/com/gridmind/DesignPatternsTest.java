package com.gridmind;

import com.gridmind.command.*;
import com.gridmind.domain.GridStatus;
import com.gridmind.domain.RiskLevel;
import com.gridmind.entity.AlertEntity;
import com.gridmind.entity.TransformerEntity;
import com.gridmind.factory.IndustrialGridFactory;
import com.gridmind.factory.UrbanGridFactory;
import com.gridmind.repository.AlertRepository;
import com.gridmind.repository.CommandLogRepository;
import com.gridmind.repository.MaintenanceRecommendationRepository;
import com.gridmind.repository.TransformerRepository;
import com.gridmind.state.*;
import com.gridmind.strategy.EvaluationResult;
import com.gridmind.strategy.ThresholdStrategy;
import com.gridmind.strategy.WeightedAgeStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

        when(transformerRepository.findById("t-123")).thenReturn(Optional.of(transformer));
        when(transformerRepository.save(any(TransformerEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        CommandInvoker invoker = new CommandInvoker(commandLogRepository);
        GridCommand command = new SimulateFailureCommand(transformerRepository, "t-123");

        // Execute Command
        invoker.executeCommand(command, "t-123", "param", "OPERATOR");
        assertEquals(GridStatus.FAILED, transformer.getStatus());
        assertEquals(RiskLevel.CRITICAL, transformer.getRiskLevel());

        // Undo Command (Restores snapshot via Memento)
        boolean undone = invoker.undoLastCommand();
        assertTrue(undone);
        assertEquals(GridStatus.NORMAL, transformer.getStatus());
        assertEquals(RiskLevel.LOW, transformer.getRiskLevel());
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
