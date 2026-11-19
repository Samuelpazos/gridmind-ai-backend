package com.gridmind.service;

import com.gridmind.dto.DashboardOverviewDto;
import com.gridmind.dto.GridSummaryDto;
import com.gridmind.dto.RiskRankingDto;
import com.gridmind.dto.SubstationDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GridDashboardFacade {

    private final GridMonitoringService monitoringService;

    public GridDashboardFacade(GridMonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    public DashboardOverviewDto getOverview() {
        return DashboardOverviewDto.builder()
                .summary(getSummary())
                .substations(getSubstations())
                .riskRanking(getRiskRanking())
                .build();
    }

    public List<SubstationDto> getSubstations() {
        return monitoringService.getFullGridHierarchy();
    }

    public GridSummaryDto getSummary() {
        return monitoringService.getGridSummary();
    }

    public List<RiskRankingDto> getRiskRanking() {
        return monitoringService.getRiskRanking();
    }
}
