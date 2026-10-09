package com.gridmind.service;

import com.gridmind.strategy.EvaluationStrategy;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AiConfigService {

    private final ApplicationContext applicationContext;
    private String activeStrategyName = "SUPERVISED_MODEL_STRATEGY";
    private boolean forceDegradedMode = false;

    public AiConfigService(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public EvaluationStrategy getActiveStrategy() {
        if (forceDegradedMode) {
            return (EvaluationStrategy) applicationContext.getBean("THRESHOLD_STRATEGY");
        }
        try {
            return (EvaluationStrategy) applicationContext.getBean(activeStrategyName);
        } catch (Exception e) {
            return (EvaluationStrategy) applicationContext.getBean("THRESHOLD_STRATEGY");
        }
    }

    public String getActiveStrategyName() {
        return forceDegradedMode ? "THRESHOLD_STRATEGY (DEGRADED_MODE)" : activeStrategyName;
    }

    public void setActiveStrategyName(String strategyName) {
        if (strategyName != null) {
            this.activeStrategyName = strategyName;
        }
    }

    public boolean isForceDegradedMode() {
        return forceDegradedMode;
    }

    public void setForceDegradedMode(boolean forceDegradedMode) {
        this.forceDegradedMode = forceDegradedMode;
    }
}
