package com.gridmind.domain;

import com.gridmind.entity.TransformerEntity;

public final class TransformerNode extends GridNodeSupport {

    public TransformerNode(TransformerEntity transformer) {
        super(
                transformer.getId(),
                transformer.getName(),
                transformer.getCode(),
                transformer.getStatus(),
                transformer.getFailureProbability() == null ? 0.0 : transformer.getFailureProbability()
        );
        if (transformer.getSensors() != null) {
            transformer.getSensors().forEach(sensor -> addComponent(new SensorNode(sensor)));
        }
    }
}
