package com.gridmind.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;

@Entity
@Table(name = "transformer_models")
@Getter
@Setter
@NoArgsConstructor
public class TransformerModelEntity {
    @Id
    private String id;
    private String manufacturer;
    @Column(name = "model_code")
    private String modelCode;
    @Column(name = "rated_power_kva")
    private double ratedPowerKva;
    @Column(name = "nominal_voltage_kv")
    private double nominalVoltageKv;
    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = java.util.UUID.randomUUID().toString();
        if (createdAt == null) createdAt = ZonedDateTime.now();
    }
}
