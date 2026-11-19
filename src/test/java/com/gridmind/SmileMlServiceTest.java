package com.gridmind;

import com.gridmind.ai.SmileMlService;
import com.gridmind.ai.SyntheticDatasetGenerator;
import com.gridmind.domain.RiskLevel;
import com.gridmind.entity.TransformerEntity;
import com.gridmind.strategy.EvaluationResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SmileMlServiceTest {

    @Test
    void trainsModelsAndCalculatesMetricsFromHoldoutSamples() {
        SmileMlService service = new SmileMlService(new SyntheticDatasetGenerator());
        service.trainModelsOnStartup();

        assertTrue(service.isModelTrained(), service.getTrainingError());
        assertMetric(service.getModelPrecision());
        assertMetric(service.getModelRecall());
        assertMetric(service.getModelF1Score());
        assertMetric(service.getModelRocAuc());
        assertTrue(service.getModelRecall() > 0.85,
                "Recall was " + service.getModelRecall());
        assertEquals(2_000, service.getTrainingDatasetSize());
        assertEquals(500, service.getTestDatasetSize());
        assertEquals(500, sum(service.getConfusionMatrix()));
        assertEquals(1.0, service.getFeatureImportances().values().stream()
                .mapToDouble(Double::doubleValue).sum(), 0.000001);
    }

    @Test
    void classifiesNormalReferenceAndExtremeOperatingConditions() {
        SmileMlService service = new SmileMlService(new SyntheticDatasetGenerator());
        service.trainModelsOnStartup();
        assertTrue(service.isModelTrained(), service.getTrainingError());

        EvaluationResult normal = service.predictFailureProbability(
                transformer(5),
                Map.of("TEMPERATURE", 50.0, "VOLTAGE", 13.8, "CURRENT", 220.0, "VIBRATION", 1.0)
        );
        assertEquals(RiskLevel.LOW, normal.getRiskLevel());

        EvaluationResult reference = service.predictFailureProbability(
                transformer(14),
                Map.of("TEMPERATURE", 87.0, "VOLTAGE", 13.8, "CURRENT", 420.0, "VIBRATION", 5.8)
        );
        assertEquals(RiskLevel.HIGH, reference.getRiskLevel(),
                "Reference probability was " + reference.getFailureProbability());
        assertTrue(reference.getFailureProbability() >= 0.70
                && reference.getFailureProbability() < 0.85,
                "Reference scenario probability was " + reference.getFailureProbability());

        EvaluationResult extreme = service.predictFailureProbability(
                transformer(25),
                Map.of("TEMPERATURE", 105.0, "VOLTAGE", 11.5, "CURRENT", 700.0, "VIBRATION", 10.0)
        );
        assertTrue(extreme.getRiskLevel() == RiskLevel.HIGH
                || extreme.getRiskLevel() == RiskLevel.CRITICAL);
    }

    @Test
    void labelsSamplesByTheirPhysicalScenarioAndIsReproducible() {
        SyntheticDatasetGenerator generator = new SyntheticDatasetGenerator();

        var first = generator.generateSyntheticDataset(100);
        var second = generator.generateSyntheticDataset(100);

        assertEquals(first, second);
        assertTrue(first.stream().allMatch(sample ->
                sample.failureLabel() == (sample.scenario() == SyntheticDatasetGenerator.Scenario.NORMAL ? 0 : 1)));
    }

    private static TransformerEntity transformer(int ageYears) {
        return TransformerEntity.builder().ageYears(ageYears).build();
    }

    private static void assertMetric(double metric) {
        assertTrue(Double.isFinite(metric));
        assertTrue(metric >= 0.0 && metric <= 1.0);
    }

    private static int sum(int[][] matrix) {
        return matrix[0][0] + matrix[0][1] + matrix[1][0] + matrix[1][1];
    }
}
