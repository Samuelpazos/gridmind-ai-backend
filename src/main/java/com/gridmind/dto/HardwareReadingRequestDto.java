package com.gridmind.dto;

import com.gridmind.domain.SensorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.ZonedDateTime;

public record HardwareReadingRequestDto(
        @NotBlank @Size(max = 50) String sensorCode,
        @NotNull Double value,
        @NotBlank @Size(max = 20) String unit,
        ZonedDateTime timestamp,
        SensorType demonstrationChannel) {

    public HardwareReadingRequestDto(String sensorCode, Double value, String unit, ZonedDateTime timestamp) {
        this(sensorCode, value, unit, timestamp, null);
    }
}
