package com.gridmind.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AiStatusDto {
    private boolean enabled;
    private String provider;
    private String model;
}
