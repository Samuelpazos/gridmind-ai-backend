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
@Table(name = "sensor_reading_batches")
@Getter
@Setter
@NoArgsConstructor
public class SensorReadingBatchEntity {
    @Id
    private String id;
    private String source;
    @Column(name = "owner_account_id")
    private String ownerAccountId;
    @Column(name = "reading_count")
    private int readingCount;
    @Column(name = "received_at")
    private ZonedDateTime receivedAt;
    @Column(name = "processed_at")
    private ZonedDateTime processedAt;
    private String status;
    @Column(name = "error_message")
    private String errorMessage;

    @PrePersist
    public void prePersist() {
        if (id == null) id = java.util.UUID.randomUUID().toString();
        if (receivedAt == null) receivedAt = ZonedDateTime.now();
    }
}
