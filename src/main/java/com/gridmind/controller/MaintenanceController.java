package com.gridmind.controller;

import com.gridmind.dto.MaintenanceDto;
import com.gridmind.service.MaintenanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/maintenances")
@Tag(name = "Maintenance", description = "Endpoints para la gestión de recomendaciones y mantenimientos de la red")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    public MaintenanceController(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @GetMapping
    @Operation(summary = "Obtener lista de recomendaciones y mantenimientos programados")
    public ResponseEntity<List<MaintenanceDto>> getMaintenances() {
        return ResponseEntity.ok(maintenanceService.getAllMaintenances());
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Marcar un mantenimiento como completado y retornar el transformador a estado NORMAL")
    public ResponseEntity<MaintenanceDto> completeMaintenance(@PathVariable String id, @RequestBody(required = false) Map<String, String> body) {
        String resultNotes = body != null ? body.get("resultNotes") : "Mantenimiento preventivo completado.";
        return ResponseEntity.ok(maintenanceService.completeMaintenance(id, resultNotes));
    }
}
