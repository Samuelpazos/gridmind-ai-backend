package com.gridmind.command;

import com.gridmind.entity.AlertEntity;
import com.gridmind.repository.AlertRepository;

import java.time.ZonedDateTime;

public class AcknowledgeAlertCommand implements GridCommand {

    private final AlertRepository alertRepository;
    private final String alertId;
    private final String operator;
    private Boolean previousState;

    public AcknowledgeAlertCommand(AlertRepository alertRepository, String alertId, String operator) {
        this.alertRepository = alertRepository;
        this.alertId = alertId;
        this.operator = operator != null ? operator : "OPERATOR";
    }

    @Override
    public String getCommandName() {
        return "ACKNOWLEDGE_ALERT";
    }

    @Override
    public void execute() {
        AlertEntity alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));
        this.previousState = alert.getAcknowledged();
        alert.setAcknowledged(true);
        alert.setAcknowledgedBy(operator);
        alert.setAcknowledgedAt(ZonedDateTime.now());
        alertRepository.save(alert);
    }

    @Override
    public void undo() {
        if (previousState != null) {
            AlertEntity alert = alertRepository.findById(alertId).orElse(null);
            if (alert != null) {
                alert.setAcknowledged(previousState);
                alert.setAcknowledgedBy(null);
                alert.setAcknowledgedAt(null);
                alertRepository.save(alert);
            }
        }
    }

    @Override
    public boolean isUndoable() {
        return true;
    }
}
