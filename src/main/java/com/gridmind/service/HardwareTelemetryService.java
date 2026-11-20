package com.gridmind.service;

import com.gridmind.config.WorkspaceDataSourceConfig;
import com.gridmind.domain.SensorType;
import com.gridmind.domain.TransformerAssetType;
import com.gridmind.dto.HardwareReadingRequestDto;
import com.gridmind.dto.SensorReadingDto;
import com.gridmind.dto.TelemetryStatusDto;
import com.gridmind.entity.SensorEntity;
import com.gridmind.entity.SensorReadingBatchEntity;
import com.gridmind.mapper.EntityDtoMapper;
import com.gridmind.repository.SensorReadingBatchRepository;
import com.gridmind.repository.SensorReadingRepository;
import com.gridmind.repository.SensorRepository;
import com.gridmind.repository.UserAccountRepository;
import com.gridmind.simulation.SensorSimulator;
import com.gridmind.telemetry.ReadingHandler;
import com.gridmind.telemetry.ReadingHandler.ReadingInput;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class HardwareTelemetryService {

    private static final int MAX_BATCH_SIZE = 32;
    private static final String DEMO_SENSOR_PREFIX = "DEMO:";

    private final SensorRepository sensorRepository;
    private final SensorReadingBatchRepository readingBatchRepository;
    private final SensorReadingRepository sensorReadingRepository;
    private final EntityDtoMapper mapper;
    private final SensorSimulator sensorSimulator;
    private final ReadingHandler readingHandler;
    private final String deviceToken;
    private final String deviceOwnerEmail;
    private final UserAccountRepository accountRepository;

    public HardwareTelemetryService(
            SensorRepository sensorRepository,
            SensorReadingBatchRepository readingBatchRepository,
            SensorReadingRepository sensorReadingRepository,
            EntityDtoMapper mapper,
            SensorSimulator sensorSimulator,
            ReadingHandler readingHandler,
            UserAccountRepository accountRepository,
            @Value("${telemetry.device-token:}") String deviceToken,
            @Value("${telemetry.device-owner-email}") String deviceOwnerEmail) {
        this.sensorRepository = sensorRepository;
        this.readingBatchRepository = readingBatchRepository;
        this.sensorReadingRepository = sensorReadingRepository;
        this.mapper = mapper;
        this.sensorSimulator = sensorSimulator;
        this.readingHandler = readingHandler;
        this.deviceToken = deviceToken == null ? "" : deviceToken.trim();
        this.deviceOwnerEmail = deviceOwnerEmail.trim().toLowerCase(Locale.ROOT);
        this.accountRepository = accountRepository;
    }

    public boolean isConfigured() {
        return deviceToken.length() >= 32 && !deviceOwnerEmail.isEmpty();
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
        ZonedDateTime now = ZonedDateTime.now();
        List<String> recentSources = sensorReadingRepository.findDistinctSourcesBetween(
                now.minusSeconds(60), now);
        ZonedDateTime lastHardwareReadingAt = sensorReadingRepository
                .findFirstBySourceOrderByTimestampDesc("HARDWARE")
                .map(reading -> reading.getTimestamp())
                .orElse(null);
        var account = WorkspaceDataSourceConfig.currentAccount();
        return new TelemetryStatusDto(
                isConfigured() && (account == null || deviceOwnerEmail.equals(account.email())),
                sensorSimulator.isActive(),
                "El modelo local se entrenó con datos sintéticos. Valida y etiqueta mediciones de la maqueta antes de interpretar sus predicciones como desempeño real.",
                resolveRecentDataSource(recentSources),
                lastHardwareReadingAt
        );
    }

    private static String resolveRecentDataSource(List<String> sources) {
        Set<String> normalizedSources = sources.stream()
                .map(source -> source.toUpperCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());
        boolean hasSimulation = normalizedSources.contains("SIMULATION")
                || normalizedSources.contains("SIMULATED");
        boolean hasHardware = normalizedSources.contains("HARDWARE");

        if (hasSimulation && hasHardware) return "MIXED";
        if (hasSimulation) return "SIMULATION";
        if (hasHardware) return "HARDWARE";
        return "NONE";
    }

    @Transactional
    public List<SensorReadingDto> ingestBatch(List<HardwareReadingRequestDto> requests) {
        var owner = accountRepository.findByEmail(deviceOwnerEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "La cuenta propietaria de la telemetría no está configurada."));
        if (sensorSimulator.isActiveFor(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Pausa el simulador antes de recibir lecturas físicas para no mezclar datos simulados y de hardware.");
        }
        if (requests == null || requests.isEmpty() || requests.size() > MAX_BATCH_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El lote debe incluir entre 1 y " + MAX_BATCH_SIZE + " lecturas.");
        }

        Set<String> seenSensors = new HashSet<>();
        List<ReadingInput> pending = new java.util.ArrayList<>(requests.size());
        ZonedDateTime now = ZonedDateTime.now();

        for (HardwareReadingRequestDto request : requests) {
            String sensorCode = request.sensorCode().trim();
            if (!seenSensors.add(sensorCode + ":" + request.demonstrationChannel())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El lote contiene más de una lectura para el sensor " + sensorCode + ".");
            }
            SensorEntity sensor = sensorRepository.findByCodeAndTransformerArchivedAtIsNull(sensorCode)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "No existe un sensor registrado con código " + sensorCode + "."));

            var transformer = sensor.getTransformer();
            if (transformer.getAssetType() != TransformerAssetType.HARDWARE
                    || !owner.getId().equals(transformer.getSubstation().getOwnerAccountId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El sensor no pertenece a este dispositivo.");
            }

            if (sensorCode.startsWith(DEMO_SENSOR_PREFIX)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Envía los canales de demostración con el código del sensor de temperatura.");
            }
            String source = "HARDWARE";
            if (request.demonstrationChannel() != null) {
                sensor = resolveDemonstrationSensor(sensor, request.demonstrationChannel());
                source = ReadingHandler.DEMONSTRATION_SOURCE;
            }
            ZonedDateTime timestamp = request.timestamp() == null ? now : request.timestamp();
            pending.add(new ReadingInput(sensor, request.value(), request.unit().trim(), timestamp, source));
        }

        SensorReadingBatchEntity batch = new SensorReadingBatchEntity();
        batch.setSource("HARDWARE");
        batch.setOwnerAccountId(owner.getId());
        batch.setReadingCount(pending.size());
        batch.setStatus("RECEIVED");
        readingBatchRepository.saveAndFlush(batch);

        List<SensorReadingDto> response = readingHandler.handleBatch(pending).stream()
                .map(mapper::toDto)
                .toList();
        batch.setStatus("PROCESSED");
        batch.setProcessedAt(ZonedDateTime.now());
        readingBatchRepository.save(batch);
        return response;
    }

    private SensorEntity resolveDemonstrationSensor(SensorEntity anchor, SensorType channel) {
        if (anchor.getType() != SensorType.TEMPERATURE || channel == SensorType.TEMPERATURE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Los canales de demostración requieren un sensor de temperatura vinculado.");
        }
        sensorRepository.lockSensor(anchor.getId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "El sensor de temperatura ya no está disponible."));
        var transformer = anchor.getTransformer();
        String code = DEMO_SENSOR_PREFIX + anchor.getId() + "-" + channel.name().substring(0, 3);
        return sensorRepository.findByCode(code)
                .filter(sensor -> transformer.getId().equals(sensor.getTransformer().getId())
                        && sensor.getType() == channel)
                .orElseGet(() -> sensorRepository.saveAndFlush(SensorEntity.builder()
                        .transformer(transformer)
                        .code(code)
                        .type(channel)
                        .unit(channel.getDefaultUnit())
                        .build()));
    }
}
