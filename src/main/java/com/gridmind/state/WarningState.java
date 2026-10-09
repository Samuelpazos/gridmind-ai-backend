package com.gridmind.state;

import com.gridmind.domain.GridStatus;

public class WarningState implements TransformerState {

    @Override
    public GridStatus getStatus() {
        return GridStatus.WARNING;
    }

    @Override
    public TransformerState evaluateNextState(double failureProbability, boolean maintenanceCompleted) {
        if (failureProbability >= 0.60) {
            return new CriticalState();
        } else if (failureProbability < 0.30) {
            return new NormalState();
        }
        return this; // Stay in WARNING
    }

    @Override
    public boolean canTransitionTo(GridStatus targetStatus) {
        return targetStatus == GridStatus.NORMAL || targetStatus == GridStatus.WARNING || targetStatus == GridStatus.CRITICAL;
    }
}
