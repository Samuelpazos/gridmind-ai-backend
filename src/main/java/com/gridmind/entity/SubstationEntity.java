package com.gridmind.entity;

import com.gridmind.domain.GridStatus;
import com.gridmind.domain.SubstationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "substations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubstationEntity {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubstationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GridStatus status;

    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @OneToMany(mappedBy = "substation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TransformerEntity> transformers = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (id == null) id = java.util.UUID.randomUUID().toString();
        if (createdAt == null) createdAt = ZonedDateTime.now();
        if (status == null) status = GridStatus.NORMAL;
    }
}
