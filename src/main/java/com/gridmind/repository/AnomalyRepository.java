package com.gridmind.repository;

import com.gridmind.entity.AnomalyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnomalyRepository extends JpaRepository<AnomalyEntity, String> {
    List<AnomalyEntity> findByTransformerIdOrderByDetectedAtDesc(String transformerId);
}
