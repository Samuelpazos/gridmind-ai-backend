package com.gridmind.simulation;

import com.gridmind.config.WorkspaceDataSourceConfig;
import com.gridmind.domain.SensorType;
import com.gridmind.domain.TransformerAssetType;
import com.gridmind.entity.SensorEntity;
import com.gridmind.entity.TransformerEntity;
import com.gridmind.repository.SensorRepository;
import com.gridmind.repository.TransformerRepository;
import com.gridmind.telemetry.ReadingHandler;
import com.gridmind.telemetry.ReadingHandler.ReadingInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Random;

@Component
public class SensorSimulator {

    private static final Logger log = LoggerFactory.getLogger(SensorSimulator.class);

    private final SensorRepository sensorRepository;
    private final TransformerRepository transformerRepository;
    private final ReadingHandler readingHandler;

    private static final long IDLE_LIMIT_NANOS = Duration.ofMinutes(10).toNanos();

    @Value("${gridmind.simulator.enabled:true}")
    private volatile boolean active = true;
    private final Settings system = new Settings(true);
    private final Map<String, Settings> workspaces = new ConcurrentHashMap<>();

    private static final class Settings {
        private volatile boolean enabled;
        private volatile ManualScenario manualScenario;
        private volatile SimulationScenario scenario = SimulationScenario.NORMAL;
        private volatile long lastViewed = System.nanoTime();
        private final AtomicInteger step = new AtomicInteger();

        private Settings(boolean enabled) {
            this.enabled = enabled;
        }
    }
    public record ManualScenario(String transformerId, Map<SensorType, Double> values) {}

    private final Random random = new Random();

    public SensorSimulator(SensorRepository sensorRepository,
                           TransformerRepository transformerRepository,
                           ReadingHandler readingHandler) {
        this.sensorRepository = sensorRepository;
        this.transformerRepository = transformerRepository;
        this.readingHandler = readingHandler;
    }

    @Scheduled(fixedRate = 3000) // Telemetry tick every 3 seconds (<3s latency requirement)
    @Transactional
    public void generateSensorTelemetryTick() {
        if (!active && workspaces.isEmpty()) return;

        List<SensorEntity> sensors = sensorRepository
                .findAllByTransformerAssetTypeAndTransformerArchivedAtIsNull(TransformerAssetType.TEST);
        if (sensors.isEmpty()) return;

        Map<Settings, Integer> ticks = new HashMap<>();
        for (SensorEntity sensor : sensors) {
            var transformer = sensor.getTransformer();
            String owner = transformer == null || transformer.getSubstation() == null
                    ? null : transformer.getSubstation().getOwnerAccountId();
            Settings settings = owner == null ? system : workspaces.get(owner);
            if (settings == null || owner != null && System.nanoTime() - settings.lastViewed > IDLE_LIMIT_NANOS) {
                continue;
            }
            ManualScenario manual = settings.manualScenario;
            boolean isManualTarget = manual != null && transformer != null && manual.transformerId().equals(transformer.getId());
            if (!isManualTarget && !(owner == null ? active : settings.enabled)) {
                continue;
            }

            int tick = ticks.computeIfAbsent(settings, item -> item.step.incrementAndGet());
            Double fixed = isManualTarget ? manual.values().get(sensor.getType()) : null;
            double newValue = fixed != null ? fixed : calculateSimulatedValue(
                    sensor, isManualTarget ? SimulationScenario.NORMAL : settings.scenario, tick);
            ZonedDateTime timestamp = ZonedDateTime.now();
            try {
                readingHandler.handleBatch(List.of(new ReadingInput(
                        sensor, newValue, sensor.getUnit(), timestamp, "SIMULATION")));
            } catch (ResponseStatusException exception) {
                log.warn("Skipping invalid simulated reading for sensor {}: {}",
                        sensor.getCode(), exception.getReason());
            }
        }
    }

    private double calculateSimulatedValue(SensorEntity sensor, SimulationScenario currentScenario, int stepCount) {
        SensorType type = sensor.getType();
        TransformerEntity transformer = sensor.getTransformer();
        boolean isTargetForDegradation = transformer != null && ("T-C-001".equals(transformer.getCode())
                || transformer.getSubstation() != null && transformer.getSubstation().getOwnerAccountId() != null);

        switch (currentScenario) {
            case NORMAL:
                return generateNormalValue(type);
            case GRADUAL_DEGRADATION:
                if (isTargetForDegradation) {
                    double degradationFactor = Math.min(1.0, stepCount * 0.05);
                    return generateDegradatedValue(type, degradationFactor);
                } else {
                    return generateNormalValue(type);
                }
            case SUDDEN_FAILURE:
                if (isTargetForDegradation) {
                    return generateSuddenFailureValue(type);
                } else {
                    return generateNormalValue(type);
                }
            default:
                return generateNormalValue(type);
        }
    }

    private double generateNormalValue(SensorType type) {
        return switch (type) {
            case TEMPERATURE -> Math.round((50.0 + random.nextGaussian() * 3.0) * 10.0) / 10.0;
            case VOLTAGE -> Math.round((13.8 + random.nextGaussian() * 0.1) * 10.0) / 10.0;
            case CURRENT -> Math.round((220.0 + random.nextGaussian() * 15.0) * 10.0) / 10.0;
            case VIBRATION -> Math.round((1.2 + Math.abs(random.nextGaussian() * 0.3)) * 10.0) / 10.0;
        };
    }

    private double generateDegradatedValue(SensorType type, double factor) {
        return switch (type) {
            case TEMPERATURE -> Math.round((55.0 + factor * 35.0 + random.nextGaussian() * 2.0) * 10.0) / 10.0; // Up to 90 °C
            case VOLTAGE -> Math.round((13.8 + (random.nextDouble() - 0.5) * 0.4) * 10.0) / 10.0;
            case CURRENT -> Math.round((250.0 + factor * 200.0 + random.nextGaussian() * 10.0) * 10.0) / 10.0; // Up to 450 A
            case VIBRATION -> Math.round((1.5 + factor * 4.5 + random.nextGaussian() * 0.4) * 10.0) / 10.0;   // Up to 6.0 mm/s
        };
    }

    private double generateSuddenFailureValue(SensorType type) {
        return switch (type) {
            case TEMPERATURE -> Math.round((92.0 + random.nextDouble() * 10.0) * 10.0) / 10.0; // 92 - 102 °C
            case VOLTAGE -> Math.round((14.8 + random.nextDouble() * 1.5) * 10.0) / 10.0;      // Overvoltage spike
            case CURRENT -> Math.round((580.0 + random.nextDouble() * 150.0) * 10.0) / 10.0;   // High current surge
            case VIBRATION -> Math.round((6.8 + random.nextDouble() * 3.0) * 10.0) / 10.0;     // Severe vibration
        };
    }

    @Transactional(readOnly = true)
    public ManualScenario applyManualScenario(String transformerId, Map<SensorType, Double> values) {
        var account = WorkspaceDataSourceConfig.currentAccount();
        if (account == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "Inicia sesión para aplicar un escenario.");
        }
        TransformerEntity transformer = transformerRepository.findByIdAndArchivedAtIsNull(transformerId)
                .filter(asset -> asset.getAssetType() == TransformerAssetType.TEST
                        && account.id().equals(asset.getSubstation().getOwnerAccountId()))
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,
                        "Selecciona un transformador simulado activo de tu cuenta."));
        Set<SensorType> available = transformer.getSensors().stream()
                .filter(sensor -> "ACTIVE".equalsIgnoreCase(sensor.getStatus()))
                .map(SensorEntity::getType).collect(Collectors.toSet());
        if (values == null || values.isEmpty() || !available.containsAll(values.keySet())) {
            throw new IllegalArgumentException("Indica al menos una lectura de un sensor activo del transformador.");
        }
        values.forEach((type, value) -> {
            double minimum = type == SensorType.TEMPERATURE ? -273.15 : 0.0;
            if (value == null || !Double.isFinite(value) || value < minimum || value > 1_000_000.0) {
                throw new IllegalArgumentException("Hay un valor de lectura simulado fuera del rango válido.");
            }
        });
        ManualScenario scenario = new ManualScenario(transformerId, Map.copyOf(values));
        currentSettings().manualScenario = scenario;
        return scenario;
    }

    public ManualScenario getManualScenario() {
        return currentSettings().manualScenario;
    }

    public void clearManualScenario() {
        currentSettings().manualScenario = null;
    }

    public boolean isActive() {
        Settings settings = currentSettings();
        return settings == system ? active : settings.enabled;
    }

    public boolean isActiveFor(String owner) {
        Settings settings = workspaces.get(owner);
        return settings != null && settings.enabled && System.nanoTime() - settings.lastViewed <= IDLE_LIMIT_NANOS;
    }

    public void setActive(boolean enabled) {
        Settings settings = currentSettings();
        if (settings == system) {
            active = enabled;
        } else {
            settings.enabled = enabled;
        }
    }

    public SimulationScenario getCurrentScenario() {
        return currentSettings().scenario;
    }

    public void setCurrentScenario(SimulationScenario scenario) {
        Settings settings = currentSettings();
        settings.scenario = scenario;
        settings.step.set(0);
    }

    private Settings currentSettings() {
        var account = WorkspaceDataSourceConfig.currentAccount();
        if (account == null) return system;
        Settings settings = workspaces.computeIfAbsent(account.id(), key -> new Settings(active));
        settings.lastViewed = System.nanoTime();
        return settings;
    }
}
