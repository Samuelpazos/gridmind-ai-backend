package com.gridmind.domain;

import com.gridmind.entity.SensorEntity;

public final class SensorNode extends GridNodeSupport {

    public SensorNode(SensorEntity sensor) {
        super(
                sensor.getId(),
                sensor.getType().name(),
                sensor.getCode(),
                "ACTIVE".equalsIgnoreCase(sensor.getStatus()) ? GridStatus.NORMAL : GridStatus.WARNING,
                0.0
        );
    }

    @Override
    public void addComponent(GridComponent component) {
        throw new UnsupportedOperationException("Un sensor es una hoja del árbol de componentes.");
    }
}
