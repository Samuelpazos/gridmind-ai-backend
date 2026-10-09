package com.gridmind.controller;

import com.gridmind.ai.SmileMlService;
import com.gridmind.domain.RiskLevel;
import com.gridmind.entity.TransformerEntity;
import com.gridmind.strategy.EvaluationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/demo")
@Tag(name = "Demo Scenarios", description = "Endpoint para ejecutar la prueba del Escenario de Referencia Académico (Sección 8)")
public class DemoScenarioController {

    private final SmileMlService smileMlService;

    public DemoScenarioController(SmileMlService smileMlService) {
        this.smileMlService = smileMlService;
    }

    @GetMapping("/reference-scenario")
    @Operation(summary = "Ejecutar evaluación del escenario de referencia obligatorio (Temp: 87°C, Volt: 13.8kV, Curr: 420A, Vib: Alta 5.8mm/s, Edad: 14 años)")
    public ResponseEntity<Map<String, Object>> getReferenceScenarioResult() {
        TransformerEntity mockTrans = TransformerEntity.builder()
                .code("T-DEMO-REF")
                .name("Transformador Demostración Académica")
                .ageYears(14)
                .ratingKva(1500.0)
                .build();

        Map<String, Double> values = new HashMap<>();
        values.put("TEMPERATURE", 87.0);
        values.put("VOLTAGE", 13.8);
        values.put("CURRENT", 420.0);
        values.put("VIBRATION", 5.8);

        EvaluationResult result = smileMlService.predictFailureProbability(mockTrans, values);

        Map<String, Object> response = new HashMap<>();
        response.put("scenarioName", "ESCENARIO DE REFERENCIA SECCIÓN 8");
        response.put("inputs", Map.of(
                "temperatura", "87.0 °C",
                "voltaje", "13.8 kV",
                "corriente", "420.0 A",
                "vibracion", "5.8 mm/s (Alta)",
                "edad", "14 años"
        ));
        response.put("riskLevel", result.getRiskLevel()); // Expect HIGH
        response.put("failureProbabilityPercent", String.format("%.1f%%", result.getFailureProbability() * 100)); // Expect between 70% and 85%
        response.put("topFactors", result.getTopFactors());
        response.put("explanation", result.getExplanation());
        response.put("recommendation", "Inspeccionar el sistema de refrigeración del transformador.");
        response.put("targetRequirementSatisfied", result.getRiskLevel() == RiskLevel.HIGH && result.getFailureProbability() >= 0.70 && result.getFailureProbability() <= 0.85);

        return ResponseEntity.ok(response);
    }
}
