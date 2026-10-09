package com.gridmind.dto;

import com.gridmind.domain.GridStatus;
import lombok.*;

import java.time.ZonedDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertDto {
    private String id;
    private String transformerId;
    private String transformerCode;
    private String transformerName;
    private String substationName;
    private GridStatus severity;
    private String message;
    private Boolean acknowledged;
    private String acknowledgedBy;
    private ZonedDateTime acknowledgedAt;
    private String source;
    private ZonedDateTime createdAt;
}
