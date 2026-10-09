package com.gridmind.entity;

import com.gridmind.domain.GridStatus;
import com.gridmind.domain.RiskLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "transformers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransformerEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "substation_id", nullable = false)
    private SubstationEntity substation;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "rating_kva", nullable = false)
    private Double ratingKva;

    @Column(name = "age_years", nullable = false)
    private Integer ageYears;

    @Column(name = "max_temp_c", nullable = false)
    private Double maxTempC;

    @Column(name = "max_current_a", nullable = false)
    private Double maxCurrentA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GridStatus status;

    @Column(name = "failure_probability", nullable = false)
    private Double failureProbability;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false)
    private RiskLevel riskLevel;

    @Column(name = "priority_score", nullable = false)
    private Double priorityScore;

    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @OneToMany(mappedBy = "transformer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SensorEntity> sensors = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (id == null) id = java.util.UUID.randomUUID().toString();
        if (createdAt == null) createdAt = ZonedDateTime.now();
        if (status == null) status = GridStatus.NORMAL;
        if (failureProbability == null) failureProbability = 0.05;
        if (riskLevel == null) riskLevel = RiskLevel.LOW;
        if (priorityScore == null) priorityScore = 5.0;
        if (ratingKva == null) ratingKva = 1000.0;
        if (ageYears == null) ageYears = 5;
        if (maxTempC == null) maxTempC = 90.0;
        if (maxCurrentA == null) maxCurrentA = 500.0;
    }
}
