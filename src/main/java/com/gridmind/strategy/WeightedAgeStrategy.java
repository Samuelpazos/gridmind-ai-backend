package com.gridmind.strategy;

import com.gridmind.domain.RiskLevel;
import com.gridmind.entity.TransformerEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component("WEIGHTED_AGE_STRATEGY")
public class WeightedAgeStrategy implements EvaluationStrategy {

    @Override
    public String getStrategyName() {
        return "WEIGHTED_AGE_STRATEGY";
    }

    @Override
    public EvaluationResult evaluate(TransformerEntity transformer, Map<String, Double> sensorValues) {
        int age = transformer != null && transformer.getAgeYears() != null ? transformer.getAgeYears() : 5;
        double temp = sensorValues.getOrDefault("TEMPERATURE", 50.0);
        double vib = sensorValues.getOrDefault("VIBRATION", 1.0);

        double baseProbability = Math.min(0.85, (age / 25.0)); // Age factor
        double tempStress = temp > 70.0 ? (temp - 70.0) * 0.015 : 0.0;
        double vibStress = vib > 2.0 ? (vib - 2.0) * 0.05 : 0.0;

        double totalProbability = Math.min(0.99, Math.max(0.01, baseProbability + tempStress + vibStress));
        RiskLevel risk = RiskLevel.fromProbability(totalProbability);

        List<String> factors = new ArrayList<>();
        factors.add("Antigüedad del transformador: " + age + " años (peso: " + String.format("%.0f%%", baseProbability * 100) + ")");
        if (tempStress > 0) factors.add("Estrés térmico acumulado (" + temp + " °C)");
        if (vibStress > 0) factors.add("Estrés mecánico acumulado (" + vib + " mm/s)");

        return EvaluationResult.builder()
                .failureProbability(totalProbability)
                .riskLevel(risk)
                .anomalyScore(totalProbability * 0.7)
                .topFactors(factors)
                .explanation("Estrategia ponderada por edad y desgaste acumulado.")
                .strategyName(getStrategyName())
                .fallbackUsed(false)
                .build();
    }
}
