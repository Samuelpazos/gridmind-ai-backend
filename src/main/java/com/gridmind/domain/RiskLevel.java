package com.gridmind.domain;

public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public static RiskLevel fromProbability(double probability) {
        if (probability < 0.30) {
            return LOW;
        } else if (probability < 0.60) {
            return MEDIUM;
        } else if (probability < 0.85) {
            return HIGH;
        } else {
            return CRITICAL;
        }
    }
}
