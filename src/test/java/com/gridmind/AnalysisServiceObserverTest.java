package com.gridmind;

import com.gridmind.observer.AnalysisServiceObserver;
import com.gridmind.observer.SensorEvent;
import com.gridmind.observer.SensorSubject;
import com.gridmind.service.GridMonitoringService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class AnalysisServiceObserverTest {

    @Test
    void analyzesEachTransformerOnlyOncePerTelemetryBatch() {
        GridMonitoringService monitoringService = mock(GridMonitoringService.class);
        AnalysisServiceObserver observer = new AnalysisServiceObserver(new SensorSubject(), monitoringService);

        observer.onSensorReadings(List.of(
                SensorEvent.builder().readingId("reading-1").transformerId("transformer-1").build(),
                SensorEvent.builder().readingId("reading-2").transformerId("transformer-1").build(),
                SensorEvent.builder().readingId("reading-3").transformerId("transformer-2").build(),
                SensorEvent.builder().readingId("reading-4").build()
        ));

        verify(monitoringService).analyzeTransformerOnNewReading("transformer-1");
        verify(monitoringService).analyzeTransformerOnNewReading("transformer-2");
        verifyNoMoreInteractions(monitoringService);
    }
}
