package com.gridmind.strategy;

import com.gridmind.ai.SmileMlService;
import com.gridmind.domain.RiskLevel;
import com.gridmind.entity.TransformerEntity;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component("ISOLATION_FOREST_STRATEGY")
public class IsolationForestStrategy implements EvaluationStrategy {

    private final SmileMlService mlService;

    public IsolationForestStrategy(@Lazy SmileMlService mlService) {
        this.mlService = mlService;
    }

    @Override
    public String getStrategyName() {
        return "ISOLATION_FOREST_STRATEGY";
    }

    @Override
    public EvaluationResult evaluate(TransformerEntity transformer, Map<String, Double> sensorValues) {
        try {
            double anomalyScore = mlService.calculateAnomalyScore(transformer, sensorValues);
            double probability = Math.min(0.99, Math.max(0.01, anomalyScore * 1.15));
            RiskLevel risk = RiskLevel.fromProbability(probability);

            List<String> factors = new ArrayList<>();
            double temp = sensorValues.getOrDefault("TEMPERATURE", 50.0);
            double vib = sensorValues.getOrDefault("VIBRATION", 1.0);
            double current = sensorValues.getOrDefault("CURRENT", 250.0);

            if (temp > 75.0) factors.add("Anomalía térmica detectada (" + temp + " °C)");
            if (vib > 3.0) factors.add("Anomalía de vibración detectada (" + vib + " mm/s)");
            if (current > 380.0) factors.add("Anomalía en consumo de corriente (" + current + " A)");

            if (factors.isEmpty()) {
                factors.add("Desviación estadística respecto al perfil patrón");
            }

            return EvaluationResult.builder()
                    .failureProbability(probability)
                    .riskLevel(risk)
                    .anomalyScore(anomalyScore)
                    .topFactors(factors)
                    .explanation("Detección de anomalías no supervisada (Isolation Forest): Score " + String.format("%.2f", anomalyScore))
                    .strategyName(getStrategyName())
                    .fallbackUsed(false)
                    .build();
        } catch (Exception e) {
            // RNF-08 Fallback to ThresholdStrategy
            ThresholdStrategy fallback = new ThresholdStrategy();
            EvaluationResult res = fallback.evaluate(transformer, sensorValues);
            res.setFallbackUsed(true);
            res.setExplanation("MODO DEGRADADO: evaluación mediante reglas (Falla en Isolation Forest). " + res.getExplanation());
            return res;
        }
    }
}
