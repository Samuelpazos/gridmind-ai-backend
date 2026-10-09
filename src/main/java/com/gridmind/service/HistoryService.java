package com.gridmind.service;

import com.gridmind.entity.HistoryLogEntity;
import com.gridmind.repository.HistoryLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

@Service
public class HistoryService {

    private final HistoryLogRepository historyLogRepository;

    public HistoryService(HistoryLogRepository historyLogRepository) {
        this.historyLogRepository = historyLogRepository;
    }

    @Transactional
    public void recordEvent(String eventType, String entityType, String entityId, String description) {
        HistoryLogEntity log = HistoryLogEntity.builder()
                .eventType(eventType)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .timestamp(ZonedDateTime.now())
                .build();
        historyLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<HistoryLogEntity> getAllHistoryLogs() {
        return historyLogRepository.findAllByOrderByTimestampDesc();
    }
}
