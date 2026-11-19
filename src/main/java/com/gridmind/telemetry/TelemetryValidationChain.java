package com.gridmind.telemetry;

import com.gridmind.entity.SensorEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;

@Component
public class TelemetryValidationChain {

    private final Handler first;

    public TelemetryValidationChain() {
        Handler numericRange = new NumericRangeHandler();
        numericRange.setNext(new ActiveSensorHandler())
                .setNext(new UnitHandler())
                .setNext(new TimestampHandler());
        first = numericRange;
    }

    public void validate(SensorEntity sensor, double value, String unit,
                         ZonedDateTime timestamp, ZonedDateTime now) {
        first.validate(new Context(sensor, value, unit, timestamp, now));
    }

    private record Context(SensorEntity sensor, double value, String unit,
                           ZonedDateTime timestamp, ZonedDateTime now) {
    }

    private abstract static class Handler {
        private Handler next;

        Handler setNext(Handler handler) {
            next = handler;
            return handler;
        }

        final void validate(Context context) {
            validateCurrent(context);
            if (next != null) {
                next.validate(context);
            }
        }

        abstract void validateCurrent(Context context);
    }

    private static final class NumericRangeHandler extends Handler {
        @Override
        void validateCurrent(Context context) {
            if (!Double.isFinite(context.value()) || Math.abs(context.value()) > 1_000_000.0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La lectura de " + context.sensor().getCode()
                                + " debe ser un número finito dentro del rango admitido.");
            }
        }
    }

    private static final class ActiveSensorHandler extends Handler {
        @Override
        void validateCurrent(Context context) {
            if (!"ACTIVE".equalsIgnoreCase(context.sensor().getStatus())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El sensor " + context.sensor().getCode() + " no está activo.");
            }
        }
    }

    private static final class UnitHandler extends Handler {
        @Override
        void validateCurrent(Context context) {
            if (!context.sensor().getUnit().equals(context.unit())) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "La unidad de " + context.sensor().getCode()
                                + " debe ser " + context.sensor().getUnit() + ".");
            }
        }
    }

    private static final class TimestampHandler extends Handler {
        @Override
        void validateCurrent(Context context) {
            if (context.timestamp().isAfter(context.now().plusMinutes(1))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La fecha de la lectura no puede estar más de un minuto en el futuro.");
            }
        }
    }
}
