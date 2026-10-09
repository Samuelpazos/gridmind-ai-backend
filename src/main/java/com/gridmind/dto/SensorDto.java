package com.gridmind.dto;

import com.gridmind.domain.SensorType;
import lombok.*;

import java.time.ZonedDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorDto {
    private String id;
    private String transformerId;
    private String code;
    private SensorType type;
    private String unit;
    private String status;
    private ZonedDateTime createdAt;
    private SensorReadingDto latestReading;
}
