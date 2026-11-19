package com.gridmind.dto;

import com.gridmind.domain.SensorType;
import com.gridmind.domain.SubstationType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record GridProvisioningRequestDto(
        @NotNull SubstationType type,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 200) String location,
        @NotBlank @Size(max = 120) String transformerName,
        @NotNull @Positive Double ratingKva,
        @NotNull @Min(0) @Max(100) Integer ageYears,
        @NotEmpty @Size(max = 4) List<@NotNull SensorType> sensorTypes) {
}
