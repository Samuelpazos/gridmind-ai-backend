package com.gridmind.observer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SensorSubject {

    private static final Logger log = LoggerFactory.getLogger(SensorSubject.class);
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
        notifyObservers(List.of(event));
    }

    public void notifyObservers(List<SensorEvent> events) {
        if (events.isEmpty()) {
            return;
        }
        for (SensorObserver observer : observers) {
            if (observer instanceof BatchSensorObserver batchObserver) {
                try {
                    batchObserver.onSensorReadings(events);
                } catch (Exception exception) {
                    log.error("Observer {} failed processing a batch of {} sensor readings",
                            observer.getClass().getSimpleName(), events.size(), exception);
                }
                continue;
            }
            for (SensorEvent event : events) {
                try {
                    observer.onSensorReading(event);
                } catch (Exception exception) {
                    log.error("Observer {} failed processing sensor reading {}",
                            observer.getClass().getSimpleName(), event.getReadingId(), exception);
                }
            }
        }
    }
}
