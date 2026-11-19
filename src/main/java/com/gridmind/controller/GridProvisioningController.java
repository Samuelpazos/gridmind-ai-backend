package com.gridmind.controller;

import com.gridmind.dto.GridProvisioningRequestDto;
import com.gridmind.dto.ManualTransformerRequestDto;
import com.gridmind.dto.SubstationDto;
import com.gridmind.dto.TransformerDto;
import com.gridmind.dto.TransformerNameUpdateRequestDto;
import com.gridmind.service.GridProvisioningService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/grid")
public class GridProvisioningController {

    private final GridProvisioningService gridProvisioningService;

    public GridProvisioningController(GridProvisioningService gridProvisioningService) {
        this.gridProvisioningService = gridProvisioningService;
    }

    @PostMapping("/substations")
    public ResponseEntity<SubstationDto> provisionSubstation(
            @Valid @RequestBody GridProvisioningRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gridProvisioningService.provision(request));
    }

    @PostMapping("/transformers")
    public ResponseEntity<TransformerDto> provisionTransformer(
            @Valid @RequestBody ManualTransformerRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gridProvisioningService.provisionTransformer(request));
    }

    @GetMapping("/transformers/archived")
    public ResponseEntity<List<TransformerDto>> getArchivedTransformers() {
        return ResponseEntity.ok(gridProvisioningService.getArchivedTransformers());
    }

    @PatchMapping("/transformers/{id}/temperature-sensor")
    public ResponseEntity<TransformerDto> linkTemperatureSensor(
            @PathVariable String id,
            @Valid @RequestBody TemperatureSensorLinkRequest request) {
        return ResponseEntity.ok(gridProvisioningService.linkTemperatureSensor(id, request.sensorCode()));
    }

    public record TemperatureSensorLinkRequest(@NotBlank @Size(max = 50) String sensorCode) {}

    @PatchMapping("/transformers/{id}/name")
    public ResponseEntity<TransformerDto> renameTransformer(
            @PathVariable String id,
            @Valid @RequestBody TransformerNameUpdateRequestDto request) {
        return ResponseEntity.ok(gridProvisioningService.renameTransformer(id, request.name()));
    }

    @DeleteMapping("/transformers/{id}")
    public ResponseEntity<Void> archiveTransformer(@PathVariable String id) {
        gridProvisioningService.archiveTransformer(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/transformers/{id}/restore")
    public ResponseEntity<TransformerDto> restoreTransformer(@PathVariable String id) {
        return ResponseEntity.ok(gridProvisioningService.restoreTransformer(id));
    }
}
