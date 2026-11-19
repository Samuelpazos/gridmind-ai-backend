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
    private Double deltaPercent;
    private Double targetValue;
}
