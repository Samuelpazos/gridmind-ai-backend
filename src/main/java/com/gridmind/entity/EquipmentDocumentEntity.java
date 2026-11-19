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
@Table(name = "equipment_documents")
@Getter
@Setter
@NoArgsConstructor
public class EquipmentDocumentEntity {
    @Id
    private String id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transformer_id")
    private TransformerEntity transformer;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "substation_id")
    private SubstationEntity substation;
    @Column(name = "document_type")
    private String documentType;
    private String title;
    @Column(name = "storage_uri", columnDefinition = "TEXT")
    private String storageUri;
    private String version;
    @Column(name = "uploaded_by")
    private String uploadedBy;
    @Column(name = "uploaded_at")
    private ZonedDateTime uploadedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = java.util.UUID.randomUUID().toString();
        if (uploadedAt == null) uploadedAt = ZonedDateTime.now();
    }
}
