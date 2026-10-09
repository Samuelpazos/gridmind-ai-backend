package com.gridmind.command;

import com.gridmind.entity.MaintenanceRecommendationEntity;
import com.gridmind.entity.TransformerEntity;
import com.gridmind.repository.MaintenanceRecommendationRepository;
import com.gridmind.repository.TransformerRepository;

import java.time.ZonedDateTime;

public class ScheduleMaintenanceCommand implements GridCommand {

    private final TransformerRepository transformerRepository;
    private final MaintenanceRecommendationRepository maintenanceRepository;
    private final String transformerId;
    private final String recommendationText;
    private String createdMaintenanceId;

    public ScheduleMaintenanceCommand(TransformerRepository transformerRepository,
                                       MaintenanceRecommendationRepository maintenanceRepository,
                                       String transformerId,
                                       String recommendationText) {
        this.transformerRepository = transformerRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.transformerId = transformerId;
        this.recommendationText = recommendationText != null ? recommendationText : "Mantenimiento preventivo programado.";
    }

    @Override
    public String getCommandName() {
        return "SCHEDULE_MAINTENANCE";
    }

    @Override
    public void execute() {
        TransformerEntity transformer = transformerRepository.findById(transformerId)
                .orElseThrow(() -> new IllegalArgumentException("Transformer not found: " + transformerId));

        MaintenanceRecommendationEntity rec = MaintenanceRecommendationEntity.builder()
                .transformer(transformer)
                .recommendation(recommendationText)
                .status("SCHEDULED")
                .scheduledDate(ZonedDateTime.now().plusDays(1))
                .build();

        rec = maintenanceRepository.save(rec);
        this.createdMaintenanceId = rec.getId();
    }

    @Override
    public void undo() {
        if (createdMaintenanceId != null) {
            maintenanceRepository.deleteById(createdMaintenanceId);
        }
    }

    @Override
    public boolean isUndoable() {
        return true;
    }
}
