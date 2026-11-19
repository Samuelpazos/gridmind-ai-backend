package com.gridmind.service;

import com.gridmind.domain.GridStatus;
import com.gridmind.domain.RiskLevel;
import com.gridmind.entity.MaintenanceRecommendationEntity;
import com.gridmind.entity.SubstationEntity;
import com.gridmind.entity.TransformerEntity;
import com.gridmind.mapper.EntityDtoMapper;
import com.gridmind.repository.MaintenanceRecommendationRepository;
import com.gridmind.repository.TransformerRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MaintenanceServiceTest {

    @Test
    void completingMaintenanceUsesStateContextAndRecalculatesCompositeStatus() {
        var maintenances = mock(MaintenanceRecommendationRepository.class);
        var transformers = mock(TransformerRepository.class);
        var monitoring = mock(GridMonitoringService.class);
        var substation = SubstationEntity.builder().id("substation-1").build();
        var transformer = TransformerEntity.builder()
                .id("transformer-1")
                .status(GridStatus.FAILED)
                .riskLevel(RiskLevel.CRITICAL)
                .failureProbability(0.98)
                .priorityScore(98.0)
                .substation(substation)
                .build();
        var maintenance = MaintenanceRecommendationEntity.builder()
                .id("maintenance-1")
                .transformer(transformer)
                .recommendation("Inspect transformer")
                .status("SCHEDULED")
                .build();
        when(maintenances.findById("maintenance-1")).thenReturn(Optional.of(maintenance));
        when(maintenances.save(any(MaintenanceRecommendationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transformers.save(any(TransformerEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        var service = new MaintenanceService(
                maintenances, transformers, new EntityDtoMapper(), monitoring);

        var result = service.completeMaintenance("maintenance-1", "Cooling system restored");

        assertEquals("COMPLETED", result.getStatus());
        assertEquals("Cooling system restored", result.getResultNotes());
        assertEquals(GridStatus.NORMAL, transformer.getStatus());
        assertEquals(RiskLevel.LOW, transformer.getRiskLevel());
        verify(transformers).save(transformer);
        verify(monitoring).recalculateSubstationStatus("substation-1");
    }
}
