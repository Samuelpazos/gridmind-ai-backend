package com.gridmind.service;

import com.gridmind.repository.AlertRuleRepository;
import com.gridmind.repository.EquipmentDocumentRepository;
import com.gridmind.repository.MaintenanceTechnicianRepository;
import com.gridmind.repository.SensorCalibrationRepository;
import com.gridmind.repository.SensorReadingBatchRepository;
import com.gridmind.repository.SensorThresholdRepository;
import com.gridmind.repository.SensorTypeCatalogRepository;
import com.gridmind.repository.TransformerModelRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OperationalCatalogServiceTest {

    @Test
    void reportsPersistedCatalogAndOperationalTableCounts() {
        var models = mock(TransformerModelRepository.class);
        var types = mock(SensorTypeCatalogRepository.class);
        var calibrations = mock(SensorCalibrationRepository.class);
        var thresholds = mock(SensorThresholdRepository.class);
        var batches = mock(SensorReadingBatchRepository.class);
        var rules = mock(AlertRuleRepository.class);
        var technicians = mock(MaintenanceTechnicianRepository.class);
        var documents = mock(EquipmentDocumentRepository.class);
        when(models.count()).thenReturn(2L);
        when(types.count()).thenReturn(4L);
        when(calibrations.count()).thenReturn(3L);
        when(thresholds.count()).thenReturn(5L);
        when(batches.count()).thenReturn(6L);
        when(rules.count()).thenReturn(7L);
        when(technicians.count()).thenReturn(8L);
        when(documents.count()).thenReturn(9L);

        var service = new OperationalCatalogService(
                models, types, calibrations, thresholds, batches, rules, technicians, documents);

        var inventory = service.getCatalogInventory();
        assertEquals(2L, inventory.get("transformerModels"));
        assertEquals(4L, inventory.get("sensorTypes"));
        assertEquals(3L, inventory.get("sensorCalibrations"));
        assertEquals(5L, inventory.get("sensorThresholds"));
        assertEquals(6L, inventory.get("sensorReadingBatches"));
        assertEquals(7L, inventory.get("alertRules"));
        assertEquals(8L, inventory.get("maintenanceTechnicians"));
        assertEquals(9L, inventory.get("equipmentDocuments"));
    }
}
