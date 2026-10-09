package com.gridmind.service;

import com.gridmind.domain.GridStatus;
import com.gridmind.dto.AlertDto;
import com.gridmind.entity.AlertEntity;
import com.gridmind.entity.TransformerEntity;
import com.gridmind.mapper.EntityDtoMapper;
import com.gridmind.observer.SensorEvent;
import com.gridmind.repository.AlertRepository;
import com.gridmind.repository.TransformerRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private final TransformerRepository transformerRepository;
    private final EntityDtoMapper mapper;
    private final SimpMessagingTemplate messagingTemplate;

    public AlertService(AlertRepository alertRepository,
                        TransformerRepository transformerRepository,
                        EntityDtoMapper mapper,
                        SimpMessagingTemplate messagingTemplate) {
        this.alertRepository = alertRepository;
        this.transformerRepository = transformerRepository;
        this.mapper = mapper;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public AlertDto createAlertIfNotExists(String transformerId, GridStatus severity, String message, String source) {
        // Prevent duplicate unacknowledged alerts for same transformer & severity
        List<AlertEntity> existing = alertRepository.findByTransformerIdAndSeverityAndAcknowledgedFalse(transformerId, severity);
        if (!existing.isEmpty()) {
            return mapper.toDto(existing.get(0));
        }

        TransformerEntity transformer = transformerRepository.findById(transformerId).orElse(null);
        if (transformer == null) return null;

        AlertEntity alert = AlertEntity.builder()
                .transformer(transformer)
                .severity(severity)
                .message(message)
                .acknowledged(false)
                .source(source)
                .build();

        alert = alertRepository.save(alert);
        AlertDto dto = mapper.toDto(alert);

        // Broadcast alert via WebSocket immediately
        messagingTemplate.convertAndSend("/topic/alerts", dto);

        return dto;
    }

    @Transactional(readOnly = true)
    public List<AlertDto> getAllAlerts() {
        return alertRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AlertDto getAlert(String alertId) {
        AlertEntity alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));
        return mapper.toDto(alert);
    }

    @Transactional
    public AlertDto acknowledgeAlert(String alertId, String operator) {
        AlertEntity alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));
        alert.setAcknowledged(true);
        alert.setAcknowledgedBy(operator != null ? operator : "OPERATOR");
        alert.setAcknowledgedAt(ZonedDateTime.now());
        alert = alertRepository.save(alert);
        return mapper.toDto(alert);
    }

    public void evaluateInstantSensorAlert(SensorEvent event) {
        if (event.getValue() == null || event.getTransformerId() == null) return;

        double val = event.getValue();
        switch (event.getSensorType()) {
            case TEMPERATURE:
                if (val > 88.0) {
                    createAlertIfNotExists(event.getTransformerId(), GridStatus.CRITICAL,
                            "Temperatura crítica detectada en " + event.getSensorCode() + ": " + val + " °C", "SENSOR_OBSERVER");
                } else if (val > 76.0) {
                    createAlertIfNotExists(event.getTransformerId(), GridStatus.WARNING,
                            "Temperatura elevada en " + event.getSensorCode() + ": " + val + " °C", "SENSOR_OBSERVER");
                }
                break;
            case VIBRATION:
                if (val > 5.0) {
                    createAlertIfNotExists(event.getTransformerId(), GridStatus.CRITICAL,
                            "Vibración severa en " + event.getSensorCode() + ": " + val + " mm/s", "SENSOR_OBSERVER");
                }
                break;
            case CURRENT:
                if (val > 480.0) {
                    createAlertIfNotExists(event.getTransformerId(), GridStatus.WARNING,
                            "Sobrecarga de corriente en " + event.getSensorCode() + ": " + val + " A", "SENSOR_OBSERVER");
                }
                break;
        }
    }
}
