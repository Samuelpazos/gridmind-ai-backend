package com.gridmind.state;

import com.gridmind.domain.GridStatus;

public class FailedState implements TransformerState {

    @Override
    public GridStatus getStatus() {
        return GridStatus.FAILED;
    }

    @Override
    public TransformerState evaluateNextState(double failureProbability, boolean maintenanceCompleted) {
        if (maintenanceCompleted && failureProbability < 0.30) {
            return new NormalState();
        }
        return this; // Cannot exit FAILED state without completed maintenance
    }

    @Override
    public boolean canTransitionTo(GridStatus targetStatus) {
        return targetStatus == GridStatus.FAILED || targetStatus == GridStatus.NORMAL;
    }
}
