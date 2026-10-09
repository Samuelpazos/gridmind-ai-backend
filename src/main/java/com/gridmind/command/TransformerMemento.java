package com.gridmind.command;

import com.gridmind.domain.GridStatus;
import com.gridmind.domain.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Memento Pattern: Preserves the internal state of a Transformer for undo functionality.
 */
@Getter
@AllArgsConstructor
@Builder
public class TransformerMemento {
    private final String transformerId;
    private final GridStatus status;
    private final RiskLevel riskLevel;
    private final Double failureProbability;
    private final Double priorityScore;
}
