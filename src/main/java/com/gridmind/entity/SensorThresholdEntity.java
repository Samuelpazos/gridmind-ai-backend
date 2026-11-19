package com.gridmind.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;

@Entity
@Table(name = "sensor_thresholds")
@Getter
@Setter
@NoArgsConstructor
public class SensorThresholdEntity {
    @Id
    private String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sensor_id", nullable = false)
    private SensorEntity sensor;
    @Column(name = "warning_min")
    private Double warningMin;
    @Column(name = "warning_max")
    private Double warningMax;
    @Column(name = "critical_min")
    private Double criticalMin;
    @Column(name = "critical_max")
    private Double criticalMax;
    @Column(name = "effective_from")
    private ZonedDateTime effectiveFrom;
    private boolean active;

    @PrePersist
    public void prePersist() {
        if (id == null) id = java.util.UUID.randomUUID().toString();
        if (effectiveFrom == null) effectiveFrom = ZonedDateTime.now();
    }
}
