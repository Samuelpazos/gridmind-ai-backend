package com.gridmind.command;

import com.gridmind.entity.AlertEntity;
import com.gridmind.repository.AlertRepository;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

public class AcknowledgeAlertCommand implements GridCommand {

    private final AlertRepository alertRepository;
    private final String alertId;
    private final String operator;
    private Boolean previousState;
    private String previousAcknowledgedBy;
    private ZonedDateTime previousAcknowledgedAt;

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
        this.previousAcknowledgedBy = alert.getAcknowledgedBy();
        this.previousAcknowledgedAt = alert.getAcknowledgedAt();
        alert.setAcknowledged(true);
        alert.setAcknowledgedBy(operator);
        alert.setAcknowledgedAt(ZonedDateTime.now());
        alertRepository.save(alert);
    }

    @Override
    public void undo() {
        if (previousState != null) {
            restore(previousState, previousAcknowledgedBy, previousAcknowledgedAt);
        }
    }

    @Override
    public Map<String, Object> createMemento() {
        if (previousState == null) {
            throw new IllegalStateException("No se puede guardar el memento antes de ejecutar el comando.");
        }
        Map<String, Object> state = new HashMap<>();
        state.put("alertId", alertId);
        state.put("acknowledged", previousState);
        state.put("acknowledgedBy", previousAcknowledgedBy);
        state.put("acknowledgedAt", previousAcknowledgedAt == null ? null : previousAcknowledgedAt.toString());
        return state;
    }

    @Override
    public void undo(Map<String, Object> state) {
        if (!alertId.equals(state.get("alertId"))) {
            throw new IllegalArgumentException("El memento no corresponde a la alerta del comando.");
        }
        Object acknowledged = state.get("acknowledged");
        if (!(acknowledged instanceof Boolean wasAcknowledged)) {
            throw new IllegalArgumentException("El memento no contiene el estado de confirmación de la alerta.");
        }
        String acknowledgedBy = (String) state.get("acknowledgedBy");
        Object acknowledgedAt = state.get("acknowledgedAt");
        ZonedDateTime acknowledgedAtValue = acknowledgedAt instanceof String value
                ? ZonedDateTime.parse(value)
                : null;
        restore(wasAcknowledged, acknowledgedBy, acknowledgedAtValue);
    }

    private void restore(Boolean acknowledged, String acknowledgedBy, ZonedDateTime acknowledgedAt) {
        AlertEntity alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));
        alert.setAcknowledged(acknowledged);
        alert.setAcknowledgedBy(acknowledgedBy);
        alert.setAcknowledgedAt(acknowledgedAt);
        alertRepository.save(alert);
    }

    @Override
    public boolean isUndoable() {
        return true;
    }
}
