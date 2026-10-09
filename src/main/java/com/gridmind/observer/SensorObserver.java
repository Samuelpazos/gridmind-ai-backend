package com.gridmind.observer;

/**
 * Observer Pattern Interface: Sensor reading subscribers implement this interface.
 */
public interface SensorObserver {
    void onSensorReading(SensorEvent event);
    String getObserverName();
}
