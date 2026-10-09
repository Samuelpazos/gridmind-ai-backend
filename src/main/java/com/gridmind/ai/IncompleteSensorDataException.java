package com.gridmind.ai;

public class IncompleteSensorDataException extends RuntimeException {
    public IncompleteSensorDataException(String transformerId) {
        super("Latest readings are incomplete for transformer: " + transformerId);
    }
}
