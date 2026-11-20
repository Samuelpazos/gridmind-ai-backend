package com.gridmind.service;

import com.gridmind.command.*;
import com.gridmind.domain.GridStatus;
import com.gridmind.dto.CommandRequestDto;
import com.gridmind.repository.AlertRepository;
import com.gridmind.repository.MaintenanceRecommendationRepository;
import com.gridmind.repository.TransformerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class CommandService {

    private final CommandInvoker commandInvoker;
    private final AlertService alertService;
    private final TransformerRepository transformerRepository;
    private final AlertRepository alertRepository;
    private final MaintenanceRecommendationRepository maintenanceRepository;

    public CommandService(CommandInvoker commandInvoker,
                          TransformerRepository transformerRepository,
                          AlertRepository alertRepository,
                          AlertService alertService,
                          MaintenanceRecommendationRepository maintenanceRepository) {
        this.commandInvoker = commandInvoker;
        this.transformerRepository = transformerRepository;
        this.alertRepository = alertRepository;
        this.alertService = alertService;
        this.maintenanceRepository = maintenanceRepository;
    }

    @Transactional
    public void executeCommand(CommandRequestDto request, String operator) {
        GridCommand command = createCommand(request.getCommandName(), request.getTargetId(),
                request.getParameters(), operator);
        commandInvoker.executeCommand(command, request.getTargetId(),
                request.getParameters() != null ? request.getParameters().toString() : "", operator);
        if (command instanceof SimulateFailureCommand) {
            alertService.createAlertIfNotExists(request.getTargetId(), GridStatus.CRITICAL,
                    "Falla simulada desde Operaciones.", "MANUAL_FAILURE_SIMULATION");
        }
    }

    @Transactional
    public boolean undoLastCommand(String operator) {
        return commandInvoker.undoLastCommand(operator, log -> createCommand(
                log.getCommandName(), log.getTargetId(), null, operator));
    }

    private GridCommand createCommand(String name, String targetId,
                                      Map<String, Object> parameters, String operator) {
        return switch (name) {
            case "SIMULATE_FAILURE" -> new SimulateFailureCommand(transformerRepository, targetId);
            case "SCHEDULE_MAINTENANCE" -> new ScheduleMaintenanceCommand(
                    transformerRepository, maintenanceRepository, targetId,
                    getRecommendation(parameters));
            case "ACKNOWLEDGE_ALERT" -> new AcknowledgeAlertCommand(alertRepository, targetId, operator);
            default -> throw new IllegalArgumentException("Unknown command: " + name);
        };
    }

    private String getRecommendation(Map<String, Object> parameters) {
        if (parameters == null || parameters.get("recommendation") == null) {
            return "Mantenimiento programado via comando.";
        }
        Object recommendation = parameters.get("recommendation");
        if (!(recommendation instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException("La recomendación de mantenimiento debe ser texto no vacío.");
        }
        return text;
    }
}
