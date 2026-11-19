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
@Table(name = "sensor_calibrations")
@Getter
@Setter
@NoArgsConstructor
public class SensorCalibrationEntity {
    @Id
    private String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sensor_id", nullable = false)
    private SensorEntity sensor;
    @Column(name = "calibrated_at", nullable = false)
    private ZonedDateTime calibratedAt;
    @Column(name = "expires_at")
    private ZonedDateTime expiresAt;
    @Column(name = "performed_by", nullable = false)
    private String performedBy;
    @Column(nullable = false)
    private String result;
    @Column(name = "certificate_number")
    private String certificateNumber;
    private String notes;

    @PrePersist
    public void prePersist() {
        if (id == null) id = java.util.UUID.randomUUID().toString();
    }
}
