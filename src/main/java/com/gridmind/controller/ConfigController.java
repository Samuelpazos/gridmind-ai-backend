package com.gridmind.controller;

import com.gridmind.domain.SensorType;
import com.gridmind.dto.GridProvisioningRequestDto;
import com.gridmind.dto.SimulatorConfigDto;
import com.gridmind.dto.SubstationDto;
import com.gridmind.dto.StrategyConfigDto;
import com.gridmind.service.AiConfigService;
import com.gridmind.service.GridProvisioningService;
import com.gridmind.simulation.SensorSimulator;
import com.gridmind.simulation.SimulationScenario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/config")
@Tag(name = "Configuration", description = "Endpoints para configurar el simulador y la estrategia de IA")
public class ConfigController {

    private final AiConfigService aiConfigService;
    private final SensorSimulator sensorSimulator;
    private final GridProvisioningService gridProvisioningService;

    public ConfigController(AiConfigService aiConfigService, SensorSimulator sensorSimulator,
                            GridProvisioningService gridProvisioningService) {
        this.aiConfigService = aiConfigService;
        this.sensorSimulator = sensorSimulator;
        this.gridProvisioningService = gridProvisioningService;
    }

    @PostMapping("/grid")
    @Operation(summary = "Crear una subestación, un transformador y sus sensores con Abstract Factory")
    public ResponseEntity<SubstationDto> provisionGrid(@Valid @RequestBody GridProvisioningRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gridProvisioningService.provision(request));
    }

    @PutMapping("/strategy")
    @Operation(summary = "Cambiar la estrategia de evaluación de IA (Patrón Strategy)")
    public ResponseEntity<StrategyConfigDto> setStrategy(@RequestBody StrategyConfigDto dto) {
        if (dto.getStrategyName() != null) {
            aiConfigService.setActiveStrategyName(dto.getStrategyName());
        }
        if (dto.getForceDegradedMode() != null) {
            aiConfigService.setForceDegradedMode(dto.getForceDegradedMode());
        }
        return ResponseEntity.ok(StrategyConfigDto.builder()
                .strategyName(aiConfigService.getActiveStrategyName())
                .forceDegradedMode(aiConfigService.isForceDegradedMode())
                .build());
    }

    @PutMapping("/simulator")
    @Operation(summary = "Configurar el simulador de sensores (Activar/desactivar, cambiar escenario)")
    public ResponseEntity<SimulatorConfigDto> setSimulator(@RequestBody SimulatorConfigDto dto) {
        if (dto.getActive() != null) {
            sensorSimulator.setActive(dto.getActive());
        }
        if (dto.getScenario() != null) {
            sensorSimulator.setCurrentScenario(SimulationScenario.valueOf(dto.getScenario().toUpperCase()));
        }
        return ResponseEntity.ok(SimulatorConfigDto.builder()
                .active(sensorSimulator.isActive())
                .scenario(sensorSimulator.getCurrentScenario().name())
                .manualScenario(sensorSimulator.getManualScenario())
                .intervalSeconds(3)
                .build());
    }

    @PutMapping("/simulator/manual")
    public ResponseEntity<SensorSimulator.ManualScenario> applyManualScenario(
            @Valid @RequestBody ManualScenarioRequest request) {
        return ResponseEntity.ok(sensorSimulator.applyManualScenario(request.transformerId(), request.values()));
    }

    @DeleteMapping("/simulator/manual")
    public ResponseEntity<Void> clearManualScenario() {
        sensorSimulator.clearManualScenario();
        return ResponseEntity.noContent().build();
    }

    public record ManualScenarioRequest(
            @NotBlank @Size(max = 36) String transformerId,
            @NotEmpty @Size(max = 4) Map<SensorType, Double> values) {}

    @GetMapping("/simulator")
    @Operation(summary = "Consultar si la telemetría de demostración simulada está activa")
    public ResponseEntity<SimulatorConfigDto> getSimulatorConfig() {
        return ResponseEntity.ok(SimulatorConfigDto.builder()
                .active(sensorSimulator.isActive())
                .scenario(sensorSimulator.getCurrentScenario().name())
                .manualScenario(sensorSimulator.getManualScenario())
                .intervalSeconds(3)
                .build());
    }
}
