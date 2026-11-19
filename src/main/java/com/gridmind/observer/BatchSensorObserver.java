package com.gridmind.observer;

import java.util.List;

public interface BatchSensorObserver extends SensorObserver {
    void onSensorReadings(List<SensorEvent> events);

    @Override
    default void onSensorReading(SensorEvent event) {
        onSensorReadings(List.of(event));
    }
}
