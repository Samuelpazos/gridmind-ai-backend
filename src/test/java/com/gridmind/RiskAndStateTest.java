package com.gridmind;

import com.gridmind.ai.SmileMlService;
import com.gridmind.ai.SyntheticDatasetGenerator;
import com.gridmind.domain.GridStatus;
import com.gridmind.domain.RiskLevel;
import com.gridmind.entity.TransformerEntity;
import com.gridmind.strategy.EvaluationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class RiskAndStateTest {

    private SmileMlService mlService;

    @BeforeEach
    void setUp() {
        SyntheticDatasetGenerator generator = new SyntheticDatasetGenerator();
        mlService = new SmileMlService(generator);
        mlService.trainModelsOnStartup();
    }

    @Test
    @DisplayName("Verify Section 8 Reference Scenario: Temp=87°C, Volt=13.8kV, Curr=420A, Vib=5.8mm/s, Age=14y -> Risk HIGH, Prob 70-85%")
    void testReferenceScenarioSection8() {
        TransformerEntity mockTransformer = TransformerEntity.builder()
                .code("T-REF-TEST")
                .name("Reference Transformer")
                .ageYears(14)
                .build();

        Map<String, Double> sensorValues = Map.of(
                "TEMPERATURE", 87.0,
                "VOLTAGE", 13.8,
                "CURRENT", 420.0,
                "VIBRATION", 5.8
        );

        EvaluationResult result = mlService.predictFailureProbability(mockTransformer, sensorValues);

        assertEquals(RiskLevel.HIGH, result.getRiskLevel(), "Risk level must be HIGH");
        assertTrue(result.getFailureProbability() >= 0.70 && result.getFailureProbability() <= 0.85,
                "Probability must be between 70% and 85%. Got: " + result.getFailureProbability());
        assertNotNull(result.getTopFactors());
        assertFalse(result.getTopFactors().isEmpty());
    }

    @Test
    @DisplayName("Verify ML Model Recall Requirement > 85%")
    void testMlModelRecallRequirement() {
        assertTrue(mlService.getModelRecall() >= 0.85, "Recall on failure class must be greater than 85%. Got: " + mlService.getModelRecall());
    }

    @Test
    @DisplayName("Verify Substation Worst Status Calculation")
    void testSubstationWorstStatus() {
        assertEquals(GridStatus.WARNING, GridStatus.worstOf(GridStatus.NORMAL, GridStatus.WARNING));
        assertEquals(GridStatus.CRITICAL, GridStatus.worstOf(GridStatus.WARNING, GridStatus.CRITICAL));
        assertEquals(GridStatus.FAILED, GridStatus.worstOf(GridStatus.CRITICAL, GridStatus.FAILED));
    }
}
