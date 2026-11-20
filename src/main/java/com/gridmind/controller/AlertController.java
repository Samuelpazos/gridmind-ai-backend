package com.gridmind.controller;

import com.gridmind.ai.ClaudeExplanationService;
import com.gridmind.ai.ClaudeNotConfiguredException;
import com.gridmind.ai.ClaudeQuotaExceededException;
import com.gridmind.ai.ClaudeServiceException;
import com.gridmind.dto.AiStatusDto;
import com.gridmind.dto.AlertDto;
import com.gridmind.dto.AlertExplanationDto;
import com.gridmind.service.AlertService;
import com.gridmind.service.AlertAssistantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@Tag(name = "Alerts", description = "Endpoints para consultar y reconocer alertas del sistema")
public class AlertController {

    private final AlertService alertService;
    private final ClaudeExplanationService claudeExplanationService;
    private final AlertAssistantContext assistantContext;

    public AlertController(AlertService alertService, ClaudeExplanationService claudeExplanationService,
                           AlertAssistantContext assistantContext) {
        this.assistantContext = assistantContext;
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
    public ResponseEntity<AlertDto> acknowledgeAlert(@PathVariable String id, Authentication authentication) {
        return ResponseEntity.ok(alertService.acknowledgeAlert(id, authentication.getName()));
    }

    @GetMapping("/ai/status")
    @Operation(summary = "Consultar si Claude está configurado")
    public ResponseEntity<AiStatusDto> getAiStatus() {
        int remainingRequests = claudeExplanationService.getRemainingRequests();
        return ResponseEntity.ok(new AiStatusDto(
                claudeExplanationService.isConfigured() && remainingRequests > 0,
                "Anthropic Claude",
                claudeExplanationService.getModel(),
                remainingRequests
        ));
    }

    @PostMapping("/{id}/assistant")
    public Map<String, Object> askAssistant(@PathVariable String id, @Valid @RequestBody AssistantQuestion request) {
        AlertAssistantContext.Context context;
        try {
            context = assistantContext.load(id);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró la alerta solicitada.");
        }
        AlertDto alert = context.alert();
        boolean simulated = alert.getAssetType() == com.gridmind.domain.TransformerAssetType.TEST;
        String origin = simulated ? " Es una prueba con un transformador simulado." : "";
        String provider = "Datos de GridMind";
        String answer;
        switch (request.topic() == null ? "" : request.topic()) {
            case "event" -> answer = alert.getMessage() + origin;
            case "time" -> answer = "La fecha que ves en la ficha corresponde al registro de la alerta. "
                    + "Con estos datos no se puede determinar cuándo comenzó la condición." + origin;
            case "next" -> answer = context.readings().isEmpty()
                    ? "Revisa el mensaje de la alerta y si el sensor tiene lecturas recientes en Activos." + origin
                    : "Compara las lecturas cercanas de la ficha con el historial del sensor en Activos. "
                    + "Verifica si el cambio se repite o fue una lectura aislada." + origin;
            default -> {
                if (request.question() == null || request.question().isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escribe una pregunta.");
                }
                try {
                    answer = claudeExplanationService.answerAlert(alert, context.readings().toString(),
                            request.question().trim());
                    provider = "Anthropic Claude";
                } catch (ClaudeNotConfiguredException | ClaudeQuotaExceededException exception) {
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                            "Las preguntas libres necesitan Claude disponible. Puedes usar las preguntas rápidas.");
                } catch (ClaudeServiceException exception) {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                            "Claude no respondió. Puedes usar las preguntas rápidas.");
                }
            }
        }
        return Map.of("answer", answer, "provider", provider, "readings", context.readings());
    }

    public record AssistantQuestion(@Size(max = 20) String topic, @Size(max = 500) String question) {}

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
        } catch (ClaudeQuotaExceededException exception) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(java.util.Map.of("message", exception.getMessage()));
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
