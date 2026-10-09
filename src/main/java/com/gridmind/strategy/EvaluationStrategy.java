package com.gridmind.strategy;

import com.gridmind.entity.TransformerEntity;

import java.util.Map;

/**
 * Strategy Pattern Interface: Allows dynamic switching between AI and Rule-based risk evaluation models.
 */
public interface EvaluationStrategy {
    String getStrategyName();
    EvaluationResult evaluate(TransformerEntity transformer, Map<String, Double> sensorValues);
}
