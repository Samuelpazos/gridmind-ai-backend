package com.gridmind.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "maintenance_technicians")
@Getter
@Setter
@NoArgsConstructor
public class MaintenanceTechnicianEntity {
    @Id
    private String id;
    @Column(name = "employee_code")
    private String employeeCode;
    private String name;
    private String email;
    private String phone;
    private String specialization;
    private boolean active;
}
