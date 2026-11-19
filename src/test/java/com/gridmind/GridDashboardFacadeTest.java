package com.gridmind;

import com.gridmind.dto.GridSummaryDto;
import com.gridmind.service.GridDashboardFacade;
import com.gridmind.service.GridMonitoringService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GridDashboardFacadeTest {

    @Test
    void consolidatesGridSummaryHierarchyAndRiskRanking() {
        GridMonitoringService monitoringService = mock(GridMonitoringService.class);
        GridSummaryDto summary = GridSummaryDto.builder().build();
        when(monitoringService.getGridSummary()).thenReturn(summary);
        when(monitoringService.getFullGridHierarchy()).thenReturn(List.of());
        when(monitoringService.getRiskRanking()).thenReturn(List.of());

        var overview = new GridDashboardFacade(monitoringService).getOverview();

        assertSame(summary, overview.getSummary());
        assertTrue(overview.getSubstations().isEmpty());
        assertTrue(overview.getRiskRanking().isEmpty());
        verify(monitoringService).getGridSummary();
        verify(monitoringService).getFullGridHierarchy();
        verify(monitoringService).getRiskRanking();
    }
}
