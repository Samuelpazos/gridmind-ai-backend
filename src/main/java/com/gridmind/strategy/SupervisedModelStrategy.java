package com.gridmind.strategy;

import com.gridmind.ai.SmileMlService;
import com.gridmind.domain.RiskLevel;
import com.gridmind.entity.TransformerEntity;
import org.springframework.context.annotation.Lazy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("SUPERVISED_MODEL_STRATEGY")
public class SupervisedModelStrategy implements EvaluationStrategy {

    private static final Logger log = LoggerFactory.getLogger(SupervisedModelStrategy.class);

    private final SmileMlService mlService;

    public SupervisedModelStrategy(@Lazy SmileMlService mlService) {
        this.mlService = mlService;
    }

    @Override
    public String getStrategyName() {
        return "SUPERVISED_MODEL_STRATEGY";
    }

    @Override
    public EvaluationResult evaluate(TransformerEntity transformer, Map<String, Double> sensorValues) {
        try {
            EvaluationResult result = mlService.predictFailureProbability(transformer, sensorValues);
            result.setStrategyName(getStrategyName());
            return result;
        } catch (RuntimeException e) {
            log.error("Random Forest evaluation failed; using threshold fallback", e);
            ThresholdStrategy fallback = new ThresholdStrategy();
            EvaluationResult res = fallback.evaluate(transformer, sensorValues);
            res.setFallbackUsed(true);
            res.setExplanation("MODO DEGRADADO: evaluación mediante reglas. " + res.getExplanation());
            return res;
        }
    }
}
