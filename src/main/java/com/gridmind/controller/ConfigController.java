package com.gridmind.controller;

import com.gridmind.dto.SimulatorConfigDto;
import com.gridmind.dto.StrategyConfigDto;
import com.gridmind.service.AiConfigService;
import com.gridmind.simulation.SensorSimulator;
import com.gridmind.simulation.SimulationScenario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/config")
@Tag(name = "Configuration", description = "Endpoints para configurar el simulador y la estrategia de IA")
public class ConfigController {

    private final AiConfigService aiConfigService;
    private final SensorSimulator sensorSimulator;

    public ConfigController(AiConfigService aiConfigService, SensorSimulator sensorSimulator) {
        this.aiConfigService = aiConfigService;
        this.sensorSimulator = sensorSimulator;
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
                .intervalSeconds(3)
                .build());
    }

    @GetMapping("/simulator")
    @Operation(summary = "Consultar si la telemetría de demostración simulada está activa")
    public ResponseEntity<SimulatorConfigDto> getSimulatorConfig() {
        return ResponseEntity.ok(SimulatorConfigDto.builder()
                .active(sensorSimulator.isActive())
                .scenario(sensorSimulator.getCurrentScenario().name())
                .intervalSeconds(3)
                .build());
    }
}
