package com.gridmind.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sensor_types")
@Getter
@Setter
@NoArgsConstructor
public class SensorTypeCatalogEntity {
    @Id
    private String id;
    private String code;
    private String name;
    @Column(name = "default_unit")
    private String defaultUnit;
    private String description;
    private boolean active;
}
