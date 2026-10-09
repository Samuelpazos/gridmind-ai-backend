package com.gridmind.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "auth_bootstrap")
@Getter
@Setter
@NoArgsConstructor
public class AuthBootstrapEntity {

    @Id
    private Integer id;

    @Column(name = "bootstrap_complete", nullable = false)
    private boolean bootstrapComplete;
}
