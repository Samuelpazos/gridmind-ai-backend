package com.gridmind.dto;

import java.time.ZonedDateTime;

public record TelemetryStatusDto(
        boolean hardwareIngestionEnabled,
        boolean simulatorActive,
        String modelValidationNotice,
        String recentDataSource,
        ZonedDateTime lastHardwareReadingAt) {
}
