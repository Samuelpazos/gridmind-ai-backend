package com.gridmind.controller;

import com.gridmind.entity.HistoryLogEntity;
import com.gridmind.service.HistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/history")
@Tag(name = "History", description = "Endpoints para consultar el historial de auditoría de eventos")
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    @Operation(summary = "Obtener el registro histórico de eventos de la red")
    public ResponseEntity<List<HistoryLogEntity>> getHistory() {
        return ResponseEntity.ok(historyService.getAllHistoryLogs());
    }
}
