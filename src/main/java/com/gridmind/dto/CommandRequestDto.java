package com.gridmind.dto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommandRequestDto {
    private String commandName; // SIMULATE_FAILURE, SCHEDULE_MAINTENANCE, ACKNOWLEDGE_ALERT
    private String targetId;
    private Map<String, Object> parameters;
    private String operator;
}
