package com.gridmind.controller;

import com.gridmind.service.AiConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.Map;

@RestController
@RequestMapping
@Tag(name = "Health", description = "Endpoint de estado de salud del sistema")
public class HealthController {

    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);
    private final AiConfigService aiConfigService;
    private final DataSource dataSource;

    public HealthController(AiConfigService aiConfigService, DataSource dataSource) {
        this.aiConfigService = aiConfigService;
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    @Operation(summary = "Verificar estado de salud general del backend")
    public ResponseEntity<Map<String, Object>> getHealth() {
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.isValid(2)) {
                return databaseUnavailable();
            }
        } catch (SQLException exception) {
            logger.error("Health check could not connect to PostgreSQL", exception);
            return databaseUnavailable();
        }

        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "system", "GridMind AI Backend",
                "timestamp", ZonedDateTime.now(),
                "degradedMode", aiConfigService.isForceDegradedMode(),
                "activeStrategy", aiConfigService.getActiveStrategyName(),
                "database", "PostgreSQL Connected",
                "websocket", "ACTIVE"
        ));
    }

    private ResponseEntity<Map<String, Object>> databaseUnavailable() {
        return ResponseEntity.status(503).body(Map.of(
                "status", "DOWN",
                "system", "GridMind AI Backend",
                "timestamp", ZonedDateTime.now(),
                "database", "Unavailable"
        ));
    }
}
