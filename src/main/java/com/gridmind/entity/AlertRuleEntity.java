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
@Table(name = "alert_rules")
@Getter
@Setter
@NoArgsConstructor
public class AlertRuleEntity {
    @Id
    private String id;
    private String code;
    private String name;
    @Column(name = "sensor_type")
    private String sensorType;
    private String severity;
    @Column(name = "condition_expression", columnDefinition = "TEXT")
    private String conditionExpression;
    private boolean enabled;
    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = java.util.UUID.randomUUID().toString();
        if (createdAt == null) createdAt = ZonedDateTime.now();
    }
}
