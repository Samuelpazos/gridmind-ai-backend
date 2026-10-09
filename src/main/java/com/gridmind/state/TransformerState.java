package com.gridmind.state;

import com.gridmind.domain.GridStatus;

/**
 * State Pattern Interface: Encapsulates state-specific behavior and rules for Transformer transitions.
 */
public interface TransformerState {
    GridStatus getStatus();
    TransformerState evaluateNextState(double failureProbability, boolean maintenanceCompleted);
    boolean canTransitionTo(GridStatus targetStatus);
}
