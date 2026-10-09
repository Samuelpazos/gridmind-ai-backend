package com.gridmind.controller;

import com.gridmind.ai.ClaudeExplanationService;
import com.gridmind.ai.ClaudeNotConfiguredException;
import com.gridmind.ai.ClaudeServiceException;
import com.gridmind.dto.AiStatusDto;
import com.gridmind.dto.AlertDto;
import com.gridmind.dto.AlertExplanationDto;
import com.gridmind.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@Tag(name = "Alerts", description = "Endpoints para consultar y reconocer alertas del sistema")
public class AlertController {

    private final AlertService alertService;
    private final ClaudeExplanationService claudeExplanationService;

    public AlertController(AlertService alertService, ClaudeExplanationService claudeExplanationService) {
        this.alertService = alertService;
        this.claudeExplanationService = claudeExplanationService;
    }

    @GetMapping
    @Operation(summary = "Obtener todas las alertas ordenadas por fecha")
    public ResponseEntity<List<AlertDto>> getAlerts() {
        return ResponseEntity.ok(alertService.getAllAlerts());
    }

    @PostMapping("/{id}/ack")
    @Operation(summary = "Reconocer una alerta por ID")
    public ResponseEntity<AlertDto> acknowledgeAlert(@PathVariable String id, @RequestParam(required = false, defaultValue = "OPERATOR") String operator) {
        return ResponseEntity.ok(alertService.acknowledgeAlert(id, operator));
    }

    @GetMapping("/ai/status")
    @Operation(summary = "Consultar si Claude está configurado")
    public ResponseEntity<AiStatusDto> getAiStatus() {
        return ResponseEntity.ok(new AiStatusDto(
                claudeExplanationService.isConfigured(),
                "Anthropic Claude",
                claudeExplanationService.getModel()
        ));
    }

    @PostMapping("/{id}/explanation")
    @Operation(summary = "Generar una explicación de una alerta con Claude")
    public ResponseEntity<?> explainAlert(@PathVariable String id) {
        try {
            AlertDto alert = alertService.getAlert(id);
            String explanation = claudeExplanationService.explain(alert);
            return ResponseEntity.ok(new AlertExplanationDto(
                    id,
                    "Anthropic Claude",
                    claudeExplanationService.getModel(),
                    explanation,
                    ZonedDateTime.now()
            ));
        } catch (ClaudeNotConfiguredException exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(java.util.Map.of("message", "Claude no está configurado. Añade CLAUDE_API_KEY al entorno del backend y reinícialo."));
        } catch (ClaudeServiceException exception) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(java.util.Map.of("message", "No se pudo obtener la explicación de Claude. Revisa la clave, la cuota del proveedor y los registros del backend."));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(java.util.Map.of("message", "No se encontró la alerta solicitada."));
        }
    }
}
