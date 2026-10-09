package com.gridmind.strategy;

import com.gridmind.domain.RiskLevel;
import com.gridmind.entity.TransformerEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component("THRESHOLD_STRATEGY")
public class ThresholdStrategy implements EvaluationStrategy {

    @Override
    public String getStrategyName() {
        return "THRESHOLD_STRATEGY";
    }

    @Override
    public EvaluationResult evaluate(TransformerEntity transformer, Map<String, Double> sensorValues) {
        double temp = sensorValues.getOrDefault("TEMPERATURE", 50.0);
        double voltage = sensorValues.getOrDefault("VOLTAGE", 13.8);
        double current = sensorValues.getOrDefault("CURRENT", 250.0);
        double vibration = sensorValues.getOrDefault("VIBRATION", 1.0);
        int age = transformer != null && transformer.getAgeYears() != null ? transformer.getAgeYears() : 5;

        double score = 0.05;
        List<String> factors = new ArrayList<>();

        if (temp > 85.0) {
            score += 0.35;
            factors.add("Temperatura crítica elevada (" + temp + " °C)");
        } else if (temp > 75.0) {
            score += 0.20;
            factors.add("Temperatura alta (" + temp + " °C)");
        }

        if (vibration > 4.5) {
            score += 0.25;
            factors.add("Vibración alta (" + vibration + " mm/s)");
        } else if (vibration > 3.0) {
            score += 0.10;
            factors.add("Vibración moderada (" + vibration + " mm/s)");
        }

        if (current > 400.0) {
            score += 0.20;
            factors.add("Sobrecarga de corriente (" + current + " A)");
        }

        if (age > 12) {
            score += 0.15;
            factors.add("Antigüedad elevada del transformador (" + age + " años)");
        }

        double probability = Math.min(0.99, Math.max(0.01, score));
        RiskLevel risk = RiskLevel.fromProbability(probability);

        String explanation = factors.isEmpty()
                ? "Operación dentro de límites normales de umbral."
                : "Evaluación por umbrales: " + String.join(", ", factors) + ".";

        return EvaluationResult.builder()
                .failureProbability(probability)
                .riskLevel(risk)
                .anomalyScore(probability * 0.8)
                .topFactors(factors)
                .explanation(explanation)
                .strategyName(getStrategyName())
                .fallbackUsed(false)
                .build();
    }
}
