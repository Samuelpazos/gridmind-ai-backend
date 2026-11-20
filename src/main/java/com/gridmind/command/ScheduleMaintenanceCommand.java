package com.gridmind.command;

import com.gridmind.entity.MaintenanceRecommendationEntity;
import com.gridmind.entity.TransformerEntity;
import com.gridmind.repository.MaintenanceRecommendationRepository;
import com.gridmind.repository.TransformerRepository;

import java.time.ZonedDateTime;
import java.util.Map;

public class ScheduleMaintenanceCommand implements GridCommand {

    private final TransformerRepository transformerRepository;
    private final MaintenanceRecommendationRepository maintenanceRepository;
    private final String transformerId;
    private final String recommendationText;
    private String createdMaintenanceId;
    private Receipt receipt;

    public record Receipt(String transformerCode, ZonedDateTime scheduledDate) {}

    public Receipt getReceipt() {
        return receipt;
    }

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
        TransformerEntity transformer = transformerRepository.lockActiveById(transformerId)
                .orElseThrow(() -> new IllegalArgumentException("Transformer not found: " + transformerId));

        if (maintenanceRepository.existsByTransformerIdAndStatus(transformerId, "SCHEDULED")) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "Este transformador ya tiene un mantenimiento programado.");
        }

        MaintenanceRecommendationEntity rec = MaintenanceRecommendationEntity.builder()
                .transformer(transformer)
                .recommendation(recommendationText)
                .status("SCHEDULED")
                .scheduledDate(ZonedDateTime.now().plusDays(1))
                .build();

        rec = maintenanceRepository.save(rec);
        this.createdMaintenanceId = rec.getId();
        this.receipt = new Receipt(transformer.getCode(), rec.getScheduledDate());
    }

    @Override
    public void undo() {
        if (createdMaintenanceId != null) {
            maintenanceRepository.deleteById(createdMaintenanceId);
        }
    }

    @Override
    public Map<String, Object> createMemento() {
        if (createdMaintenanceId == null) {
            throw new IllegalStateException("No se puede guardar el memento antes de ejecutar el comando.");
        }
        return Map.of("maintenanceId", createdMaintenanceId);
    }

    @Override
    public void undo(Map<String, Object> memento) {
        Object savedId = memento.get("maintenanceId");
        if (!(savedId instanceof String id) || id.isBlank()) {
            throw new IllegalArgumentException("El memento no contiene el identificador del mantenimiento.");
        }
        maintenanceRepository.deleteById(id);
    }

    @Override
    public boolean isUndoable() {
        return true;
    }
}
