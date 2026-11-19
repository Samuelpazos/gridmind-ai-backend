package com.gridmind.controller;

import com.gridmind.ai.SmileMlService;
import com.gridmind.dto.ModelInfoDto;
import com.gridmind.service.AiConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/model/info")
@Tag(name = "Model Info", description = "Endpoints para consultar las métricas del modelo de IA Smile (Precision, Recall, ROC-AUC)")
public class ModelInfoController {

    private final SmileMlService smileMlService;
    private final AiConfigService aiConfigService;

    public ModelInfoController(SmileMlService smileMlService, AiConfigService aiConfigService) {
        this.smileMlService = smileMlService;
        this.aiConfigService = aiConfigService;
    }

    @GetMapping
    @Operation(summary = "Obtener información y métricas de desempeño del modelo ML")
    public ResponseEntity<ModelInfoDto> getModelInfo() {
        boolean fallbackActive = aiConfigService.isForceDegradedMode() || !smileMlService.isModelTrained();
        String fallbackReason = aiConfigService.isForceDegradedMode()
                ? "MODO DEGRADADO ACTIVADO: evaluación mediante reglas de umbral."
                : smileMlService.isModelTrained()
                ? "Operación normal con IA."
                : "El modelo no está disponible; se usarán reglas de umbral.";
        return ResponseEntity.ok(ModelInfoDto.builder()
                .modelName("Smile Random Forest Classifier & Isolation Forest (Java Native ML)")
                .anomalyDetectorName("Smile Isolation Forest Anomaly Engine")
                .precision(smileMlService.getModelPrecision())
                .recall(smileMlService.getModelRecall())
                .f1Score(smileMlService.getModelF1Score())
                .rocAuc(smileMlService.getModelRocAuc())
                .modelTrained(smileMlService.isModelTrained())
                .trainingError(smileMlService.getTrainingError())
                .trainingSamplesCount(smileMlService.getTrainingDatasetSize())
                .testSamplesCount(smileMlService.getTestDatasetSize())
                .confusionMatrix(smileMlService.getConfusionMatrix())
                .seedUsed(42L)
                .featureImportances(smileMlService.getFeatureImportances())
                .activeStrategy(aiConfigService.getActiveStrategyName())
                .fallbackActive(fallbackActive)
                .fallbackReason(fallbackReason)
                .build());
    }
}
