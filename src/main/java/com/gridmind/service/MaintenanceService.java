package com.gridmind.service;

import com.gridmind.domain.GridStatus;
import com.gridmind.domain.RiskLevel;
import com.gridmind.dto.MaintenanceDto;
import com.gridmind.entity.MaintenanceRecommendationEntity;
import com.gridmind.entity.TransformerEntity;
import com.gridmind.mapper.EntityDtoMapper;
import com.gridmind.repository.MaintenanceRecommendationRepository;
import com.gridmind.repository.TransformerRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MaintenanceService {

    private final MaintenanceRecommendationRepository maintenanceRepository;
    private final TransformerRepository transformerRepository;
    private final EntityDtoMapper mapper;
    private final GridMonitoringService monitoringService;

    public MaintenanceService(MaintenanceRecommendationRepository maintenanceRepository,
                              TransformerRepository transformerRepository,
                              EntityDtoMapper mapper,
                              @Lazy GridMonitoringService monitoringService) {
        this.maintenanceRepository = maintenanceRepository;
        this.transformerRepository = transformerRepository;
        this.mapper = mapper;
        this.monitoringService = monitoringService;
    }

    @Transactional
    public MaintenanceDto createRecommendation(String transformerId, String text) {
        TransformerEntity transformer = transformerRepository.findById(transformerId)
                .orElseThrow(() -> new IllegalArgumentException("Transformer not found: " + transformerId));

        MaintenanceRecommendationEntity entity = MaintenanceRecommendationEntity.builder()
                .transformer(transformer)
                .recommendation(text)
                .status("PENDING")
                .build();

        entity = maintenanceRepository.save(entity);
        return mapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public List<MaintenanceDto> getAllMaintenances() {
        return maintenanceRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public MaintenanceDto completeMaintenance(String maintenanceId, String resultNotes) {
        MaintenanceRecommendationEntity rec = maintenanceRepository.findById(maintenanceId)
                .orElseThrow(() -> new IllegalArgumentException("Maintenance not found: " + maintenanceId));

        rec.setStatus("COMPLETED");
        rec.setCompletionDate(ZonedDateTime.now());
        rec.setResultNotes(resultNotes != null ? resultNotes : "Mantenimiento completado satisfactoriamente.");
        rec = maintenanceRepository.save(rec);

        // Reset transformer status back to NORMAL if maintenance completed!
        TransformerEntity transformer = rec.getTransformer();
        if (transformer != null) {
            transformer.setStatus(GridStatus.NORMAL);
            transformer.setRiskLevel(RiskLevel.LOW);
            transformer.setFailureProbability(0.06);
            transformer.setPriorityScore(6.0);
            transformerRepository.save(transformer);

            // Re-evaluate substation composite status
            monitoringService.recalculateSubstationStatus(transformer.getSubstation().getId());
        }

        return mapper.toDto(rec);
    }
}
