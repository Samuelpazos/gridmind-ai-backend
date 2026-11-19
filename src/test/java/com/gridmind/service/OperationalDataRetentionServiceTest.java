package com.gridmind.service;

import com.gridmind.repository.AnomalyRepository;
import com.gridmind.repository.PredictionRepository;
import com.gridmind.repository.SensorReadingRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class OperationalDataRetentionServiceTest {

    @Test
    void sevenDayRetentionUsesOneUtcCutoff() {
        AnomalyRepository anomalies = mock(AnomalyRepository.class);
        PredictionRepository predictions = mock(PredictionRepository.class);
        SensorReadingRepository readings = mock(SensorReadingRepository.class);
        var service = new OperationalDataRetentionService(anomalies, predictions, readings, 7,
                Clock.fixed(Instant.parse("2026-01-15T12:00:00Z"), ZoneOffset.UTC));

        service.purgeExpiredOperationalData();

        ZonedDateTime cutoff = ZonedDateTime.parse("2026-01-08T12:00Z");
        verify(anomalies).deleteOlderThan(cutoff);
        verify(predictions).deleteOlderThan(cutoff);
        verify(readings).deleteOlderThan(cutoff);
    }

    @Test
    void purgesAllConfiguredOperationalDataUsingOneUtcCutoff() {
        AnomalyRepository anomalies = mock(AnomalyRepository.class);
        PredictionRepository predictions = mock(PredictionRepository.class);
        SensorReadingRepository readings = mock(SensorReadingRepository.class);
        var service = new OperationalDataRetentionService(anomalies, predictions, readings, 30,
                Clock.fixed(Instant.parse("2026-01-15T12:00:00Z"), ZoneOffset.UTC));
        ZonedDateTime cutoff = ZonedDateTime.parse("2025-12-16T12:00Z");

        service.purgeExpiredOperationalData();

        verify(anomalies).deleteOlderThan(cutoff);
        verify(predictions).deleteOlderThan(cutoff);
        verify(readings).deleteOlderThan(cutoff);
    }

    @Test
    void rejectsNegativeRetentionDays() {
        assertThrows(IllegalArgumentException.class, () -> new OperationalDataRetentionService(
                mock(AnomalyRepository.class),
                mock(PredictionRepository.class),
                mock(SensorReadingRepository.class),
                -1,
                Clock.systemUTC()));
    }
}
