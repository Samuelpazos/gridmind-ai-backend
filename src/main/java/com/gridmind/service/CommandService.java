package com.gridmind.service;

import com.gridmind.command.*;
import com.gridmind.dto.CommandRequestDto;
import com.gridmind.repository.AlertRepository;
import com.gridmind.repository.MaintenanceRecommendationRepository;
import com.gridmind.repository.TransformerRepository;
import org.springframework.stereotype.Service;

@Service
public class CommandService {

    private final CommandInvoker commandInvoker;
    private final TransformerRepository transformerRepository;
    private final AlertRepository alertRepository;
    private final MaintenanceRecommendationRepository maintenanceRepository;

    public CommandService(CommandInvoker commandInvoker,
                          TransformerRepository transformerRepository,
                          AlertRepository alertRepository,
                          MaintenanceRecommendationRepository maintenanceRepository) {
        this.commandInvoker = commandInvoker;
        this.transformerRepository = transformerRepository;
        this.alertRepository = alertRepository;
        this.maintenanceRepository = maintenanceRepository;
    }

    public void executeCommand(CommandRequestDto request) {
        String name = request.getCommandName();
        GridCommand command;

        switch (name) {
            case "SIMULATE_FAILURE":
                command = new SimulateFailureCommand(transformerRepository, request.getTargetId());
                break;
            case "SCHEDULE_MAINTENANCE":
                String text = request.getParameters() != null ? (String) request.getParameters().get("recommendation") : "Mantenimiento programado via comando.";
                command = new ScheduleMaintenanceCommand(transformerRepository, maintenanceRepository, request.getTargetId(), text);
                break;
            case "ACKNOWLEDGE_ALERT":
                command = new AcknowledgeAlertCommand(alertRepository, request.getTargetId(), request.getOperator());
                break;
            default:
                throw new IllegalArgumentException("Unknown command: " + name);
        }

        commandInvoker.executeCommand(command, request.getTargetId(), request.getParameters() != null ? request.getParameters().toString() : "", request.getOperator());
    }

    public boolean undoLastCommand() {
        return commandInvoker.undoLastCommand();
    }
}
