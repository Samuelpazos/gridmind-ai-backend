package com.gridmind.domain;

import com.gridmind.entity.SubstationEntity;

public final class SubstationNode extends GridNodeSupport {

    public SubstationNode(SubstationEntity substation) {
        super(substation.getId(), substation.getName(), substation.getCode(), GridStatus.NORMAL, 0.0);
        if (substation.getTransformers() != null) {
            substation.getTransformers().stream()
                    .filter(transformer -> transformer.getArchivedAt() == null)
                    .forEach(transformer -> addComponent(new TransformerNode(transformer)));
        }
    }
}
