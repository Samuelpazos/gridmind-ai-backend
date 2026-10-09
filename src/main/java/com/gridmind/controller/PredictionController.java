package com.gridmind.controller;

import com.gridmind.dto.PredictionDto;
import com.gridmind.entity.PredictionEntity;
import com.gridmind.mapper.EntityDtoMapper;
import com.gridmind.repository.PredictionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/predictions")
@Tag(name = "Predictions", description = "Endpoints para consultar las predicciones de fallos de IA")
public class PredictionController {

    private final PredictionRepository predictionRepository;
    private final EntityDtoMapper mapper;

    public PredictionController(PredictionRepository predictionRepository, EntityDtoMapper mapper) {
        this.predictionRepository = predictionRepository;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "Obtener historial de predicciones de IA")
    public ResponseEntity<List<PredictionDto>> getPredictions() {
        List<PredictionEntity> list = predictionRepository.findAllByOrderByEvaluatedAtDesc(PageRequest.of(0, 50));
        return ResponseEntity.ok(list.stream().map(mapper::toDto).collect(Collectors.toList()));
    }
}
