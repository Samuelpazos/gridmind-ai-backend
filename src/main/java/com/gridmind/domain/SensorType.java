package com.gridmind.domain;

public enum SensorType {
    TEMPERATURE("°C", 90.0),
    VOLTAGE("kV", 15.0),
    CURRENT("A", 500.0),
    VIBRATION("mm/s", 5.0);

    private final String defaultUnit;
    private final double defaultThreshold;

    SensorType(String defaultUnit, double defaultThreshold) {
        this.defaultUnit = defaultUnit;
        this.defaultThreshold = defaultThreshold;
    }

    public String getDefaultUnit() {
        return defaultUnit;
    }

    public double getDefaultThreshold() {
        return defaultThreshold;
    }
}
