package com.gridmind.state;

import com.gridmind.domain.GridStatus;

public class StateContext {

    private TransformerState currentState;

    public StateContext(GridStatus status) {
        this.currentState = fromStatus(status);
    }

    public static TransformerState fromStatus(GridStatus status) {
        if (status == null) return new NormalState();
        return switch (status) {
            case NORMAL -> new NormalState();
            case WARNING -> new WarningState();
            case CRITICAL -> new CriticalState();
            case FAILED -> new FailedState();
        };
    }

    public TransformerState getCurrentState() {
        return currentState;
    }

    public GridStatus evaluate(double failureProbability, boolean maintenanceCompleted) {
        TransformerState nextState = currentState.evaluateNextState(failureProbability, maintenanceCompleted);
        this.currentState = nextState;
        return nextState.getStatus();
    }
}
