package com.gridmind.service;

import com.gridmind.repository.AlertRuleRepository;
import com.gridmind.repository.EquipmentDocumentRepository;
import com.gridmind.repository.MaintenanceTechnicianRepository;
import com.gridmind.repository.SensorCalibrationRepository;
import com.gridmind.repository.SensorReadingBatchRepository;
import com.gridmind.repository.SensorThresholdRepository;
import com.gridmind.repository.SensorTypeCatalogRepository;
import com.gridmind.repository.TransformerModelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class OperationalCatalogService {

    private final TransformerModelRepository transformerModels;
    private final SensorTypeCatalogRepository sensorTypes;
    private final SensorCalibrationRepository sensorCalibrations;
    private final SensorThresholdRepository sensorThresholds;
    private final SensorReadingBatchRepository readingBatches;
    private final AlertRuleRepository alertRules;
    private final MaintenanceTechnicianRepository maintenanceTechnicians;
    private final EquipmentDocumentRepository equipmentDocuments;

    public OperationalCatalogService(TransformerModelRepository transformerModels,
                                     SensorTypeCatalogRepository sensorTypes,
                                     SensorCalibrationRepository sensorCalibrations,
                                     SensorThresholdRepository sensorThresholds,
                                     SensorReadingBatchRepository readingBatches,
                                     AlertRuleRepository alertRules,
                                     MaintenanceTechnicianRepository maintenanceTechnicians,
                                     EquipmentDocumentRepository equipmentDocuments) {
        this.transformerModels = transformerModels;
        this.sensorTypes = sensorTypes;
        this.sensorCalibrations = sensorCalibrations;
        this.sensorThresholds = sensorThresholds;
        this.readingBatches = readingBatches;
        this.alertRules = alertRules;
        this.maintenanceTechnicians = maintenanceTechnicians;
        this.equipmentDocuments = equipmentDocuments;
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getCatalogInventory() {
        return Map.of(
                "transformerModels", transformerModels.count(),
                "sensorTypes", sensorTypes.count(),
                "sensorCalibrations", sensorCalibrations.count(),
                "sensorThresholds", sensorThresholds.count(),
                "sensorReadingBatches", readingBatches.count(),
                "alertRules", alertRules.count(),
                "maintenanceTechnicians", maintenanceTechnicians.count(),
                "equipmentDocuments", equipmentDocuments.count()
        );
    }
}
