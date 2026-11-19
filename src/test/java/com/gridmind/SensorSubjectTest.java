package com.gridmind;

import com.gridmind.observer.SensorEvent;
import com.gridmind.observer.SensorObserver;
import com.gridmind.observer.SensorSubject;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SensorSubjectTest {

    @Test
    void continuesNotifyingObserversAfterOneObserverFails() {
        SensorSubject subject = new SensorSubject();
        SensorObserver failingObserver = mock(SensorObserver.class);
        SensorObserver followingObserver = mock(SensorObserver.class);
        doThrow(new IllegalStateException("observer failed"))
                .when(failingObserver).onSensorReading(any(SensorEvent.class));
        subject.registerObserver(failingObserver);
        subject.registerObserver(followingObserver);

        subject.notifyObservers(SensorEvent.builder().readingId("reading-1").build());

        verify(followingObserver).onSensorReading(any(SensorEvent.class));
    }
}
