package com.gridmind.dto;

public record TelemetryStatusDto(
        boolean hardwareIngestionEnabled,
        boolean simulatorActive,
        String modelValidationNotice) {
}
