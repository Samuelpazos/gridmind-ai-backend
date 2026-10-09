package com.gridmind.dto;

import lombok.*;

import java.time.ZonedDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorReadingDto {
    private String id;
    private String sensorId;
    private String sensorCode;
    private String sensorType;
    private String transformerId;
    private Double value;
    private String unit;
    private ZonedDateTime timestamp;
    private String source;
}
