package com.gridmind.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommandRequestDto {
    @NotBlank
    @Pattern(regexp = "SIMULATE_FAILURE|SCHEDULE_MAINTENANCE|ACKNOWLEDGE_ALERT")
    private String commandName;
    @NotBlank
    @Size(max = 36)
    private String targetId;
    private Map<String, Object> parameters;
    private String operator;
}
