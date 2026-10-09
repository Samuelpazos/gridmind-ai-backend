package com.gridmind.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Table(name = "anomalies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnomalyEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transformer_id", nullable = false)
    private TransformerEntity transformer;

    @Column(name = "anomaly_score", nullable = false)
    private Double anomalyScore;

    @Column(name = "detected_at")
    private ZonedDateTime detectedAt;

    @Column(name = "strategy_used", nullable = false)
    private String strategyUsed;

    @Column(columnDefinition = "TEXT")
    private String details;

    @PrePersist
    public void prePersist() {
        if (id == null) id = java.util.UUID.randomUUID().toString();
        if (detectedAt == null) detectedAt = ZonedDateTime.now();
    }
}
