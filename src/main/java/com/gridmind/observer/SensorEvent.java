package com.gridmind.observer;

import com.gridmind.domain.SensorType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorEvent {
    private String readingId;
    private String sensorId;
    private String sensorCode;
    private SensorType sensorType;
    private String transformerId;
    private String transformerCode;
    private Double value;
    private String unit;
    private ZonedDateTime timestamp;
}
