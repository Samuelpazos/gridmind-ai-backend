package com.gridmind.command;

import com.gridmind.domain.GridStatus;
import com.gridmind.domain.RiskLevel;
import com.gridmind.entity.TransformerEntity;
import com.gridmind.repository.TransformerRepository;

public class SimulateFailureCommand implements GridCommand {

    private final TransformerRepository transformerRepository;
    private final String transformerId;
    private TransformerMemento memento;

    public SimulateFailureCommand(TransformerRepository transformerRepository, String transformerId) {
        this.transformerRepository = transformerRepository;
        this.transformerId = transformerId;
    }

    @Override
    public String getCommandName() {
        return "SIMULATE_FAILURE";
    }

    @Override
    public void execute() {
        TransformerEntity transformer = transformerRepository.findById(transformerId)
                .orElseThrow(() -> new IllegalArgumentException("Transformer not found: " + transformerId));

        // Create Memento snapshot
        this.memento = TransformerMemento.builder()
                .transformerId(transformer.getId())
                .status(transformer.getStatus())
                .riskLevel(transformer.getRiskLevel())
                .failureProbability(transformer.getFailureProbability())
                .priorityScore(transformer.getPriorityScore())
                .build();

        // Mutate for failure simulation test mode
        transformer.setStatus(GridStatus.FAILED);
        transformer.setRiskLevel(RiskLevel.CRITICAL);
        transformer.setFailureProbability(0.98);
        transformer.setPriorityScore(98.0);

        transformerRepository.save(transformer);
    }

    @Override
    public void undo() {
        if (memento != null) {
            TransformerEntity transformer = transformerRepository.findById(transformerId).orElse(null);
            if (transformer != null) {
                transformer.setStatus(memento.getStatus());
                transformer.setRiskLevel(memento.getRiskLevel());
                transformer.setFailureProbability(memento.getFailureProbability());
                transformer.setPriorityScore(memento.getPriorityScore());
                transformerRepository.save(transformer);
            }
        }
    }

    @Override
    public boolean isUndoable() {
        return true;
    }
}
