package com.gridmind.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.ZonedDateTime;

@Getter
@AllArgsConstructor
public class AlertExplanationDto {
    private String alertId;
    private String provider;
    private String model;
    private String explanation;
    private ZonedDateTime generatedAt;
}
