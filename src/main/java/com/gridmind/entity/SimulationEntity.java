package com.gridmind.entity;

import com.gridmind.domain.GridStatus;
import com.gridmind.domain.RiskLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Table(name = "simulations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimulationEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transformer_id", nullable = false)
    private TransformerEntity transformer;

    @Column(name = "variable_modified", nullable = false)
    private String variableModified;

    @Column(name = "delta_percent", nullable = false)
    private Double deltaPercent;

    @Enumerated(EnumType.STRING)
    @Column(name = "original_risk", nullable = false)
    private RiskLevel originalRisk;

    @Enumerated(EnumType.STRING)
    @Column(name = "simulated_risk", nullable = false)
    private RiskLevel simulatedRisk;

    @Column(name = "original_probability", nullable = false)
    private Double originalProbability;

    @Column(name = "simulated_probability", nullable = false)
    private Double simulatedProbability;

    @Enumerated(EnumType.STRING)
    @Column(name = "original_state", nullable = false)
    private GridStatus originalState;

    @Enumerated(EnumType.STRING)
    @Column(name = "simulated_state", nullable = false)
    private GridStatus simulatedState;

    @Column(columnDefinition = "TEXT")
    private String recommendation;

    @Column(name = "executed_at")
    private ZonedDateTime executedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = java.util.UUID.randomUUID().toString();
        if (executedAt == null) executedAt = ZonedDateTime.now();
    }
}
