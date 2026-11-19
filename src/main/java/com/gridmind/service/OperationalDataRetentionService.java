package com.gridmind.service;

import com.gridmind.repository.AnomalyRepository;
import com.gridmind.repository.PredictionRepository;
import com.gridmind.repository.SensorReadingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.ZonedDateTime;

@Service
public class OperationalDataRetentionService {

    private static final Logger log = LoggerFactory.getLogger(OperationalDataRetentionService.class);

    private final AnomalyRepository anomalyRepository;
    private final PredictionRepository predictionRepository;
    private final SensorReadingRepository readingRepository;
    private final int retentionDays;
    private final Clock clock;

    @Autowired
    public OperationalDataRetentionService(
            AnomalyRepository anomalyRepository,
            PredictionRepository predictionRepository,
            SensorReadingRepository readingRepository,
            @Value("${gridmind.retention.days:7}") int retentionDays) {
        this(anomalyRepository, predictionRepository, readingRepository, retentionDays, Clock.systemUTC());
    }

    OperationalDataRetentionService(
            AnomalyRepository anomalyRepository,
            PredictionRepository predictionRepository,
            SensorReadingRepository readingRepository,
            int retentionDays,
            Clock clock) {
        if (retentionDays < 0) {
            throw new IllegalArgumentException("Data retention days must be zero or greater.");
        }
        this.anomalyRepository = anomalyRepository;
        this.predictionRepository = predictionRepository;
        this.readingRepository = readingRepository;
        this.retentionDays = retentionDays;
        this.clock = clock;
    }

    @Scheduled(cron = "${gridmind.retention.cleanup-cron:0 15 3 * * *}")
    @Transactional
    public void purgeExpiredOperationalData() {
        if (retentionDays == 0) {
            return;
        }

        ZonedDateTime cutoff = ZonedDateTime.now(clock).minusDays(retentionDays);
        int anomalies = anomalyRepository.deleteOlderThan(cutoff);
        int predictions = predictionRepository.deleteOlderThan(cutoff);
        int readings = readingRepository.deleteOlderThan(cutoff);
        if (anomalies + predictions + readings > 0) {
            log.info("Purged expired operational data: {} anomalies, {} predictions, {} readings before {}",
                    anomalies, predictions, readings, cutoff);
        }
    }
}
