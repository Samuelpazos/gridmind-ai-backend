package com.gridmind.command;

import com.gridmind.domain.GridStatus;
import com.gridmind.domain.RiskLevel;
import com.gridmind.domain.TransformerAssetType;
import com.gridmind.entity.TransformerEntity;
import com.gridmind.repository.TransformerRepository;

import java.util.HashMap;
import java.util.Map;

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
        TransformerEntity transformer = transformerRepository.findByIdAndArchivedAtIsNull(transformerId)
                .orElseThrow(() -> new IllegalArgumentException("Transformer not found: " + transformerId));

        if (transformer.getAssetType() != TransformerAssetType.TEST) {
            throw new IllegalArgumentException("Solo puedes simular fallas en transformadores simulados.");
        }

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
    public Map<String, Object> createMemento() {
        if (memento == null) {
            throw new IllegalStateException("No se puede guardar el memento antes de ejecutar el comando.");
        }
        Map<String, Object> state = new HashMap<>();
        state.put("transformerId", memento.getTransformerId());
        state.put("status", memento.getStatus().name());
        state.put("riskLevel", memento.getRiskLevel().name());
        state.put("failureProbability", memento.getFailureProbability());
        state.put("priorityScore", memento.getPriorityScore());
        return state;
    }

    @Override
    public void undo() {
        if (memento != null) {
            restore(memento);
        }
    }

    @Override
    public void undo(Map<String, Object> state) {
        Object savedId = state.get("transformerId");
        if (!transformerId.equals(savedId)) {
            throw new IllegalArgumentException("El memento no corresponde al transformador del comando.");
        }
        restore(TransformerMemento.builder()
                .transformerId(transformerId)
                .status(GridStatus.valueOf((String) state.get("status")))
                .riskLevel(RiskLevel.valueOf((String) state.get("riskLevel")))
                .failureProbability(((Number) state.get("failureProbability")).doubleValue())
                .priorityScore(((Number) state.get("priorityScore")).doubleValue())
                .build());
    }

    private void restore(TransformerMemento snapshot) {
        TransformerEntity transformer = transformerRepository.findByIdAndArchivedAtIsNull(transformerId)
                .orElseThrow(() -> new IllegalArgumentException("Transformer not found: " + transformerId));
        transformer.setStatus(snapshot.getStatus());
        transformer.setRiskLevel(snapshot.getRiskLevel());
        transformer.setFailureProbability(snapshot.getFailureProbability());
        transformer.setPriorityScore(snapshot.getPriorityScore());
        transformerRepository.save(transformer);
    }

    @Override
    public boolean isUndoable() {
        return true;
    }
}
