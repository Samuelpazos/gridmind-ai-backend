package com.gridmind.dto;

import com.gridmind.simulation.SensorSimulator.ManualScenario;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimulatorConfigDto {
    private Boolean active;
    private String scenario;
    private ManualScenario manualScenario; // NORMAL, GRADUAL_DEGRADATION, SUDDEN_FAILURE
    private Integer intervalSeconds;
    private String targetTransformerId;
}
