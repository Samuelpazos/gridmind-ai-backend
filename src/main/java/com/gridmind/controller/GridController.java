package com.gridmind.controller;

import com.gridmind.ai.ClaudeNotConfiguredException;
import com.gridmind.ai.ClaudePredictionService;
import com.gridmind.ai.ClaudeServiceException;
import com.gridmind.ai.IncompleteSensorDataException;
import com.gridmind.ai.TransformerNotFoundException;
import com.gridmind.dto.*;
import com.gridmind.entity.SensorReadingEntity;
import com.gridmind.entity.TransformerEntity;
import com.gridmind.factory.GridComponentFactory;
import com.gridmind.factory.IndustrialGridFactory;
import com.gridmind.factory.UrbanGridFactory;
import com.gridmind.mapper.EntityDtoMapper;
import com.gridmind.repository.SensorReadingRepository;
import com.gridmind.repository.SensorRepository;
import com.gridmind.repository.SubstationRepository;
import com.gridmind.repository.TransformerRepository;
import com.gridmind.service.GridMonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@Tag(name = "Grid Monitoring", description = "Endpoints para el monitoreo de la red eléctrica")
public class GridController {

    private final GridMonitoringService monitoringService;
    private final TransformerRepository transformerRepository;
    private final SensorRepository sensorRepository;
    private final SensorReadingRepository readingRepository;
    private final SubstationRepository substationRepository;
    private final EntityDtoMapper mapper;
    private final UrbanGridFactory urbanGridFactory;
    private final IndustrialGridFactory industrialGridFactory;
    private final ClaudePredictionService claudePredictionService;

    public GridController(GridMonitoringService monitoringService,
                          TransformerRepository transformerRepository,
                          SensorRepository sensorRepository,
                          SensorReadingRepository readingRepository,
                          SubstationRepository substationRepository,
                          EntityDtoMapper mapper,
                          UrbanGridFactory urbanGridFactory,
                          IndustrialGridFactory industrialGridFactory,
                          ClaudePredictionService claudePredictionService) {
        this.monitoringService = monitoringService;
        this.transformerRepository = transformerRepository;
        this.sensorRepository = sensorRepository;
        this.readingRepository = readingRepository;
        this.substationRepository = substationRepository;
        this.mapper = mapper;
        this.urbanGridFactory = urbanGridFactory;
        this.industrialGridFactory = industrialGridFactory;
        this.claudePredictionService = claudePredictionService;
    }

    @GetMapping("/grid")
    @Operation(summary = "Obtener jerarquía completa de la red eléctrica")
    public ResponseEntity<List<SubstationDto>> getGrid() {
        return ResponseEntity.ok(monitoringService.getFullGridHierarchy());
    }

    @GetMapping("/grid/summary")
    @Operation(summary = "Obtener resumen estadístico del dashboard")
    public ResponseEntity<GridSummaryDto> getGridSummary() {
        return ResponseEntity.ok(monitoringService.getGridSummary());
    }

    @GetMapping("/ranking")
    @Operation(summary = "Obtener ranking de riesgo priorizado por Riesgo x Impacto")
    public ResponseEntity<List<RiskRankingDto>> getRiskRanking() {
        return ResponseEntity.ok(monitoringService.getRiskRanking());
    }

    @GetMapping("/components/{id}/readings")
    @Operation(summary = "Obtener historial de lecturas de sensores de un componente")
    public ResponseEntity<List<SensorReadingDto>> getComponentReadings(@PathVariable String id) {
        List<SensorReadingEntity> readings = readingRepository.findBySensorIdOrderByTimestampDesc(id, PageRequest.of(0, 50));
        return ResponseEntity.ok(readings.stream().map(mapper::toDto).collect(Collectors.toList()));
    }

    @GetMapping("/components/{id}/risk")
    @Operation(summary = "Obtener evaluación de riesgo de un transformador")
    public ResponseEntity<TransformerDto> getComponentRisk(@PathVariable String id) {
        TransformerEntity transformer = transformerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transformer not found: " + id));
        TransformerDto dto = mapper.toDto(transformer);
        return ResponseEntity.ok(dto);
    }

    @PostMapping({"/components/{id}/claude-prediction", "/components/{id}/gemini-prediction"})
    @Operation(summary = "Solicitar manualmente a Claude una estimación de riesgo con las últimas lecturas")
    public ResponseEntity<?> predictWithClaude(@PathVariable String id) {
        try {
            return ResponseEntity.ok(claudePredictionService.predictCurrentRisk(id));
        } catch (ClaudeNotConfiguredException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", "Claude no está configurado. Añade CLAUDE_API_KEY al entorno del backend y reinícialo."));
        } catch (ClaudeServiceException exception) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", "Claude no pudo generar la estimación. Revisa la clave, la cuota del proveedor y los registros del backend."));
        } catch (TransformerNotFoundException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No se encontró el transformador solicitado."));
        } catch (IncompleteSensorDataException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "El transformador aún no tiene lecturas recientes de todos los sensores requeridos."));
        }
    }
}
