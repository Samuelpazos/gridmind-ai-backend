package com.gridmind.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimulatorConfigDto {
    private Boolean active;
    private String scenario; // NORMAL, GRADUAL_DEGRADATION, SUDDEN_FAILURE
    private Integer intervalSeconds;
    private String targetTransformerId;
}
