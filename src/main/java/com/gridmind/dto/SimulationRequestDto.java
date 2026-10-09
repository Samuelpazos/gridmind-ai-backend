package com.gridmind.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimulationRequestDto {
    @NotNull
    private String transformerId;
    @NotNull
    private String variableModified; // TEMPERATURE, VOLTAGE, CURRENT, VIBRATION
    @NotNull
    private Double deltaPercent; // e.g. +30.0 for 30% increase
}
