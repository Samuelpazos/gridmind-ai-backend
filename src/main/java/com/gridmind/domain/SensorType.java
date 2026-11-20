package com.gridmind.domain;

public enum SensorType {
    TEMPERATURE("°C", 90.0, 76.0, 88.0),
    VOLTAGE("kV", 15.0, null, null),
    CURRENT("A", 500.0, 480.0, null),
    VIBRATION("mm/s", 5.0, null, 5.0);

    private final String defaultUnit;
    private final double defaultThreshold;

    private final Double warningAlertThreshold;
    private final Double criticalAlertThreshold;

    SensorType(String defaultUnit, double defaultThreshold, Double warningAlertThreshold, Double criticalAlertThreshold) {
        this.warningAlertThreshold = warningAlertThreshold;
        this.criticalAlertThreshold = criticalAlertThreshold;
        this.defaultUnit = defaultUnit;
        this.defaultThreshold = defaultThreshold;
    }

    public Double getWarningAlertThreshold() {
        return warningAlertThreshold;
    }

    public Double getCriticalAlertThreshold() {
        return criticalAlertThreshold;
    }

    public String getDefaultUnit() {
        return defaultUnit;
    }

    public double getDefaultThreshold() {
        return defaultThreshold;
    }
}
