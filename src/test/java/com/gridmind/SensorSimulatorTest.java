package com.gridmind;

import com.gridmind.domain.SensorType;
import com.gridmind.domain.TransformerAssetType;
import com.gridmind.entity.SensorEntity;
import com.gridmind.entity.TransformerEntity;
import com.gridmind.repository.SensorRepository;
import com.gridmind.repository.TransformerRepository;
import com.gridmind.simulation.SensorSimulator;
import com.gridmind.telemetry.ReadingHandler;
import com.gridmind.telemetry.ReadingHandler.ReadingInput;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SensorSimulatorTest {

    @Test
    void simulatedReadingsUseTheSharedReadingHandler() {
        SensorRepository sensors = mock(SensorRepository.class);
        TransformerRepository transformers = mock(TransformerRepository.class);
        ReadingHandler readingHandler = mock(ReadingHandler.class);
        SensorSimulator simulator = new SensorSimulator(sensors, transformers, readingHandler);
        SensorEntity sensor = SensorEntity.builder()
                .id("sensor-1")
                .code("SN-001-TMP")
                .type(SensorType.TEMPERATURE)
                .unit("°C")
                .status("ACTIVE")
                .transformer(TransformerEntity.builder()
                        .id("transformer-1")
                        .code("T-C-001")
                        .assetType(TransformerAssetType.TEST)
                        .build())
                .build();
        when(sensors.findAllByTransformerAssetTypeAndTransformerArchivedAtIsNull(TransformerAssetType.TEST))
                .thenReturn(List.of(sensor));

        simulator.generateSensorTelemetryTick();

        var inputs = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(readingHandler).handleBatch(inputs.capture());
        ReadingInput input = (ReadingInput) inputs.getValue().getFirst();
        assertEquals(sensor, input.sensor());
        assertEquals("SIMULATION", input.source());
    }

    @Test
    void doesNotGenerateSyntheticReadingsForHardwareSensors() {
        SensorRepository sensors = mock(SensorRepository.class);
        TransformerRepository transformers = mock(TransformerRepository.class);
        ReadingHandler readingHandler = mock(ReadingHandler.class);
        SensorSimulator simulator = new SensorSimulator(sensors, transformers, readingHandler);
        when(sensors.findAllByTransformerAssetTypeAndTransformerArchivedAtIsNull(TransformerAssetType.TEST))
                .thenReturn(List.of());

        simulator.generateSensorTelemetryTick();

        verify(sensors).findAllByTransformerAssetTypeAndTransformerArchivedAtIsNull(TransformerAssetType.TEST);
        verify(readingHandler, never()).handleBatch(org.mockito.ArgumentMatchers.<List<ReadingInput>>any());
    }
}
