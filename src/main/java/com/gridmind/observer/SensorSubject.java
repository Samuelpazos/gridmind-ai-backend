package com.gridmind.observer;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SensorSubject {

    private final List<SensorObserver> observers = new CopyOnWriteArrayList<>();

    public void registerObserver(SensorObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void unregisterObserver(SensorObserver observer) {
        observers.remove(observer);
    }

    public void notifyObservers(SensorEvent event) {
        for (SensorObserver observer : observers) {
            try {
                observer.onSensorReading(event);
            } catch (Exception e) {
                System.err.println("Error notifying observer " + observer.getObserverName() + ": " + e.getMessage());
            }
        }
    }
}
