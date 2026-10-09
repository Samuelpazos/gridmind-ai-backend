package com.gridmind.service;

import com.gridmind.dto.HardwareReadingRequestDto;
import com.gridmind.dto.SensorReadingDto;
import com.gridmind.dto.TelemetryStatusDto;
import com.gridmind.entity.SensorEntity;
import com.gridmind.entity.SensorReadingEntity;
import com.gridmind.mapper.EntityDtoMapper;
import com.gridmind.observer.SensorEvent;
import com.gridmind.observer.SensorSubject;
import com.gridmind.repository.SensorReadingRepository;
import com.gridmind.repository.SensorRepository;
import com.gridmind.simulation.SensorSimulator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class HardwareTelemetryService {

    private static final int MAX_BATCH_SIZE = 32;
    private static final double MAX_ABSOLUTE_READING = 1_000_000.0;

    private final SensorRepository sensorRepository;
    private final SensorReadingRepository readingRepository;
    private final SensorSubject sensorSubject;
    private final EntityDtoMapper mapper;
    private final SensorSimulator sensorSimulator;
    private final String deviceToken;

    public HardwareTelemetryService(
            SensorRepository sensorRepository,
            SensorReadingRepository readingRepository,
            SensorSubject sensorSubject,
            EntityDtoMapper mapper,
            SensorSimulator sensorSimulator,
            @Value("${telemetry.device-token:}") String deviceToken) {
        this.sensorRepository = sensorRepository;
        this.readingRepository = readingRepository;
        this.sensorSubject = sensorSubject;
        this.mapper = mapper;
        this.sensorSimulator = sensorSimulator;
        this.deviceToken = deviceToken == null ? "" : deviceToken.trim();
    }

    public boolean isConfigured() {
        return deviceToken.length() >= 32;
    }

    public void authenticateDevice(String suppliedToken) {
        if (!isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "La recepción de hardware no está configurada. Define TELEMETRY_DEVICE_TOKEN en el backend.");
        }
        if (suppliedToken == null || !MessageDigest.isEqual(
                deviceToken.getBytes(StandardCharsets.UTF_8),
                suppliedToken.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credencial de dispositivo inválida.");
        }
    }

    public TelemetryStatusDto getStatus() {
        return new TelemetryStatusDto(
                isConfigured(),
                sensorSimulator.isActive(),
                "El modelo local se entrenó con datos sintéticos. Valida y etiqueta mediciones de la maqueta antes de interpretar sus predicciones como desempeño real."
        );
    }

    @Transactional
    public List<SensorReadingDto> ingestBatch(List<HardwareReadingRequestDto> requests) {
        if (sensorSimulator.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Pausa el simulador antes de recibir lecturas físicas para no mezclar datos simulados y de hardware.");
        }
        if (requests == null || requests.isEmpty() || requests.size() > MAX_BATCH_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El lote debe incluir entre 1 y " + MAX_BATCH_SIZE + " lecturas.");
        }

        Set<String> seenSensors = new HashSet<>();
        List<SensorReadingEntity> pending = new ArrayList<>(requests.size());
        ZonedDateTime now = ZonedDateTime.now();

        for (HardwareReadingRequestDto request : requests) {
            String sensorCode = request.sensorCode().trim();
            if (!seenSensors.add(sensorCode)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El lote contiene más de una lectura para el sensor " + sensorCode + ".");
            }
            if (!Double.isFinite(request.value()) || Math.abs(request.value()) > MAX_ABSOLUTE_READING) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La lectura de " + sensorCode + " debe ser un número finito dentro del rango admitido.");
            }

            SensorEntity sensor = sensorRepository.findByCode(sensorCode)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "No existe un sensor registrado con código " + sensorCode + "."));
            if (!"ACTIVE".equalsIgnoreCase(sensor.getStatus())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El sensor " + sensorCode + " no está activo.");
            }
            if (!sensor.getUnit().equals(request.unit().trim())) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "La unidad de " + sensorCode + " debe ser " + sensor.getUnit() + ".");
            }

            ZonedDateTime timestamp = request.timestamp() == null ? now : request.timestamp();
            if (timestamp.isAfter(now.plusMinutes(1))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La fecha de la lectura no puede estar más de un minuto en el futuro.");
            }

            pending.add(SensorReadingEntity.builder()
                    .sensor(sensor)
                    .value(request.value())
                    .unit(sensor.getUnit())
                    .timestamp(timestamp)
                    .source("HARDWARE")
                    .build());
        }

        List<SensorReadingEntity> saved = readingRepository.saveAllAndFlush(pending);
        List<SensorReadingDto> response = new ArrayList<>(saved.size());
        for (SensorReadingEntity reading : saved) {
            SensorEntity sensor = reading.getSensor();
            var transformer = sensor.getTransformer();
            SensorEvent event = SensorEvent.builder()
                    .readingId(reading.getId())
                    .sensorId(sensor.getId())
                    .sensorCode(sensor.getCode())
                    .sensorType(sensor.getType())
                    .transformerId(transformer.getId())
                    .transformerCode(transformer.getCode())
                    .value(reading.getValue())
                    .unit(reading.getUnit())
                    .timestamp(reading.getTimestamp())
                    .build();
            sensorSubject.notifyObservers(event);
            response.add(mapper.toDto(reading));
        }
        return response;
    }
}
