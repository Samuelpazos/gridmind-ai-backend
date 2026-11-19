package com.gridmind;

import com.gridmind.domain.TransformerAssetType;
import com.gridmind.entity.AlertEntity;
import com.gridmind.entity.TransformerEntity;
import com.gridmind.mapper.EntityDtoMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntityDtoMapperTest {

    @Test
    void includesTransformerAssetTypeInAlertDto() {
        EntityDtoMapper mapper = new EntityDtoMapper();
        AlertEntity hardwareAlert = alertFor(TransformerAssetType.HARDWARE);
        AlertEntity testAlert = alertFor(TransformerAssetType.TEST);

        assertEquals(TransformerAssetType.HARDWARE, mapper.toDto(hardwareAlert).getAssetType());
        assertEquals(TransformerAssetType.TEST, mapper.toDto(testAlert).getAssetType());
    }

    private AlertEntity alertFor(TransformerAssetType assetType) {
        return AlertEntity.builder()
                .transformer(TransformerEntity.builder()
                        .id("transformer-" + assetType)
                        .assetType(assetType)
                        .build())
                .build();
    }
}
