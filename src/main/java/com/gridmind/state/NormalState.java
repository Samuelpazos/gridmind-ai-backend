package com.gridmind.state;

import com.gridmind.domain.GridStatus;

public class NormalState implements TransformerState {

    @Override
    public GridStatus getStatus() {
        return GridStatus.NORMAL;
    }

    @Override
    public TransformerState evaluateNextState(double failureProbability, boolean maintenanceCompleted) {
        if (failureProbability >= 0.60) {
            return new CriticalState();
        } else if (failureProbability >= 0.30) {
            return new WarningState();
        }
        return this; // Stay in NORMAL
    }

    @Override
    public boolean canTransitionTo(GridStatus targetStatus) {
        return targetStatus == GridStatus.NORMAL || targetStatus == GridStatus.WARNING;
    }
}
