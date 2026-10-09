package com.gridmind.controller;

import com.gridmind.dto.HardwareReadingRequestDto;
import com.gridmind.dto.SensorReadingDto;
import com.gridmind.dto.TelemetryStatusDto;
import com.gridmind.service.HardwareTelemetryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/telemetry")
@Tag(name = "Hardware Telemetry", description = "Recepción de lecturas verificadas de dispositivos de la maqueta")
public class HardwareTelemetryController {

    private final HardwareTelemetryService telemetryService;

    public HardwareTelemetryController(HardwareTelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @GetMapping("/status")
    @Operation(summary = "Consultar el modo del simulador y si el receptor de hardware está configurado")
    public ResponseEntity<TelemetryStatusDto> status() {
        return ResponseEntity.ok(telemetryService.getStatus());
    }

    @PostMapping("/readings")
    @Operation(summary = "Recibir un lote atómico de lecturas de sensores del ESP32")
    public ResponseEntity<List<SensorReadingDto>> ingestReadings(
            @RequestHeader(name = "X-Device-Token", required = false) String deviceToken,
            @NotEmpty @Size(max = 32) @Valid @RequestBody List<@Valid HardwareReadingRequestDto> readings) {
        telemetryService.authenticateDevice(deviceToken);
        return ResponseEntity.status(HttpStatus.CREATED).body(telemetryService.ingestBatch(readings));
    }
}
