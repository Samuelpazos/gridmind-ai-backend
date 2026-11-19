package com.gridmind.controller;

import com.gridmind.ai.SmileMlService;
import com.gridmind.dto.DemoAccountConfigDto;
import com.gridmind.entity.TransformerEntity;
import com.gridmind.strategy.EvaluationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/demo")
@Tag(name = "Demo Scenarios", description = "Endpoint para evaluar una entrada fija del escenario de referencia")
public class DemoScenarioController {

    private final SmileMlService smileMlService;
    private final boolean demoEnabled;
    private final String demoEmail;

    public DemoScenarioController(
            SmileMlService smileMlService,
            @Value("${auth.bootstrap.demo.enabled:true}") boolean demoEnabled,
            @Value("${auth.bootstrap.demo.email:}") String demoEmail) {
        this.smileMlService = smileMlService;
        this.demoEnabled = demoEnabled;
        this.demoEmail = demoEmail == null ? "" : demoEmail.trim();
    }

    @GetMapping("/config")
    @Operation(summary = "Consultar si la cuenta demo está configurada y obtener su correo")
    public ResponseEntity<DemoAccountConfigDto> getDemoAccountConfig() {
        boolean enabled = demoEnabled && !demoEmail.isBlank();
        return ResponseEntity.ok(new DemoAccountConfigDto(enabled, enabled ? demoEmail : null));
    }

    @GetMapping("/reference-scenario")
    @Operation(summary = "Evaluar una entrada fija de prueba con el modelo disponible")
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
        response.put("riskLevel", result.getRiskLevel());
        response.put("failureProbability", result.getFailureProbability());
        response.put("failureProbabilityPercent", String.format("%.1f%%", result.getFailureProbability() * 100));
        response.put("topFactors", result.getTopFactors());
        response.put("explanation", result.getExplanation());
        response.put("recommendation", "Inspeccionar el sistema de refrigeración del transformador.");

        return ResponseEntity.ok(response);
    }
}
