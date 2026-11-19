package com.gridmind.controller;

import com.gridmind.dto.CommandRequestDto;
import com.gridmind.service.CommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/commands")
@Tag(name = "Commands", description = "Endpoints para ejecutar comandos de operación y deshacer (Undo)")
public class CommandController {

    private final CommandService commandService;

    public CommandController(CommandService commandService) {
        this.commandService = commandService;
    }

    @PostMapping("/execute")
    @Operation(summary = "Ejecutar un comando (SIMULATE_FAILURE, SCHEDULE_MAINTENANCE, ACKNOWLEDGE_ALERT)")
    public ResponseEntity<Map<String, String>> executeCommand(
            @Valid @RequestBody CommandRequestDto request, Authentication authentication) {
        commandService.executeCommand(request, authentication.getName());
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Comando ejecutado: " + request.getCommandName()));
    }

    @PostMapping("/undo")
    @Operation(summary = "Deshacer el último comando ejecutable (Patrón Memento)")
    public ResponseEntity<Map<String, String>> undoCommand(Authentication authentication) {
        boolean success = commandService.undoLastCommand(authentication.getName());
        if (success) {
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Último comando deshecho correctamente."));
        } else {
            return ResponseEntity.badRequest().body(Map.of("status", "FAILED", "message", "No hay comandos reversibles en la pila."));
        }
    }
}
