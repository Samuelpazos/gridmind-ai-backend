package com.gridmind.state;

import com.gridmind.domain.GridStatus;

public class CriticalState implements TransformerState {

    @Override
    public GridStatus getStatus() {
        return GridStatus.CRITICAL;
    }

    @Override
    public TransformerState evaluateNextState(double failureProbability, boolean maintenanceCompleted) {
        if (failureProbability > 0.92) {
            return new FailedState();
        } else if (failureProbability < 0.60 && failureProbability >= 0.30) {
            return new WarningState();
        } else if (failureProbability < 0.30) {
            return new WarningState(); // Step down to Warning first unless maintenance done
        }
        return this; // Stay in CRITICAL
    }

    @Override
    public boolean canTransitionTo(GridStatus targetStatus) {
        return targetStatus == GridStatus.WARNING || targetStatus == GridStatus.CRITICAL || targetStatus == GridStatus.FAILED;
    }
}
