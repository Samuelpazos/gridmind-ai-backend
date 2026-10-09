package com.gridmind.dto;

import lombok.*;

import java.time.ZonedDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceDto {
    private String id;
    private String transformerId;
    private String transformerCode;
    private String transformerName;
    private String recommendation;
    private String status; // PENDING, SCHEDULED, COMPLETED
    private ZonedDateTime scheduledDate;
    private ZonedDateTime completionDate;
    private String resultNotes;
    private ZonedDateTime createdAt;
}
