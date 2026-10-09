package com.gridmind.entity;

import com.gridmind.domain.RiskLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Table(name = "predictions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictionEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transformer_id", nullable = false)
    private TransformerEntity transformer;

    @Column(name = "failure_probability", nullable = false)
    private Double failureProbability;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false)
    private RiskLevel riskLevel;

    @Column(name = "top_factors", columnDefinition = "TEXT")
    private String topFactors;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "evaluated_at")
    private ZonedDateTime evaluatedAt;

    @Column(name = "model_used", nullable = false)
    private String modelUsed;

    @PrePersist
    public void prePersist() {
        if (id == null) id = java.util.UUID.randomUUID().toString();
        if (evaluatedAt == null) evaluatedAt = ZonedDateTime.now();
    }
}
