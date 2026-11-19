package com.gridmind;

import com.gridmind.domain.GridStatus;
import com.gridmind.domain.SensorType;
import com.gridmind.domain.SubstationNode;
import com.gridmind.entity.SensorEntity;
import com.gridmind.entity.SubstationEntity;
import com.gridmind.entity.TransformerEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CompositePatternTest {

    @Test
    void aggregatesStatusRiskAndComponentCountAcrossGridHierarchy() {
        SensorEntity sensor = SensorEntity.builder()
                .id("sensor-1")
                .code("S-1")
                .type(SensorType.TEMPERATURE)
                .status("ACTIVE")
                .build();
        TransformerEntity transformer = TransformerEntity.builder()
                .id("transformer-1")
                .name("Transformer 1")
                .code("T-1")
                .status(GridStatus.CRITICAL)
                .failureProbability(0.82)
                .sensors(List.of(sensor))
                .build();
        SubstationEntity substation = SubstationEntity.builder()
                .id("substation-1")
                .name("Substation 1")
                .code("SS-1")
                .transformers(List.of(transformer))
                .build();

        SubstationNode grid = new SubstationNode(substation);

        assertEquals(GridStatus.CRITICAL, grid.getStatus());
        assertEquals(0.82, grid.getRiskScore(), 0.0001);
        assertEquals(3, grid.countComponents());
    }

    @Test
    void exposesReadOnlyChildrenAndPreventsAddingChildrenToSensors() {
        SensorEntity sensor = SensorEntity.builder()
                .id("sensor-1")
                .code("S-1")
                .type(SensorType.TEMPERATURE)
                .status("ACTIVE")
                .build();
        TransformerEntity transformer = TransformerEntity.builder()
                .id("transformer-1")
                .name("Transformer 1")
                .code("T-1")
                .status(GridStatus.NORMAL)
                .failureProbability(0.1)
                .sensors(List.of(sensor))
                .build();
        SubstationEntity substation = SubstationEntity.builder()
                .id("substation-1")
                .name("Substation 1")
                .code("SS-1")
                .transformers(List.of(transformer))
                .build();

        SubstationNode grid = new SubstationNode(substation);

        assertThrows(UnsupportedOperationException.class, () -> grid.getChildren().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> grid.getChildren().getFirst().getChildren().getFirst().addComponent(grid));
    }
}
