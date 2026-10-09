package com.gridmind.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Table(name = "command_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommandLogEntity {

    @Id
    private String id;

    @Column(name = "command_name", nullable = false)
    private String commandName;

    @Column(name = "target_id")
    private String targetId;

    @Column(columnDefinition = "TEXT")
    private String parameters;

    @Column(name = "executed_at")
    private ZonedDateTime executedAt;

    @Column(name = "executed_by")
    private String executedBy;

    private Boolean undoable;

    @PrePersist
    public void prePersist() {
        if (id == null) id = java.util.UUID.randomUUID().toString();
        if (executedAt == null) executedAt = ZonedDateTime.now();
        if (executedBy == null) executedBy = "OPERATOR";
        if (undoable == null) undoable = false;
    }
}
