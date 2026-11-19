package com.gridmind.dto;

import com.gridmind.domain.SensorType;
import com.gridmind.domain.SubstationType;
import com.gridmind.domain.TransformerAssetType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

public record ManualTransformerRequestDto(
        @Size(max = 36) String substationId,
        SubstationType substationType,
        @Size(max = 120) String substationName,
        @Size(max = 200) String substationLocation,
        @NotBlank @Size(max = 120) String transformerName,
        @Size(max = 50) String transformerCode,
        @NotNull TransformerAssetType assetType,
        @Positive Double ratingKva,
        @Min(0) @Max(100) Integer ageYears,
        @NotNull @Size(max = 4) List<@NotNull SensorType> sensorTypes,
        @Valid Map<@NotNull SensorType, @NotBlank @Size(max = 50) String> sensorCodes) {
}
