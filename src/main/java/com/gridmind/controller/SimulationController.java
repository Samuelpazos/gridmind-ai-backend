package com.gridmind.controller;

import com.gridmind.dto.SimulationRequestDto;
import com.gridmind.dto.SimulationResultDto;
import com.gridmind.service.SimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/simulations")
@Tag(name = "Simulations", description = "Endpoints para ejecutar laboratorio de simulación de fallas What-If")
public class SimulationController {

    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @PostMapping
    @Operation(summary = "Ejecutar una simulación de falla en un transformador sin alterar BD operativa")
    public ResponseEntity<SimulationResultDto> runSimulation(@Valid @RequestBody SimulationRequestDto request) {
        return ResponseEntity.ok(simulationService.executeSimulation(request));
    }

    @GetMapping
    @Operation(summary = "Obtener historial de simulaciones ejecutadas")
    public ResponseEntity<List<SimulationResultDto>> getSimulationHistory() {
        return ResponseEntity.ok(simulationService.getSimulationHistory());
    }
}
