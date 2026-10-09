package com.gridmind.repository;

import com.gridmind.domain.GridStatus;
import com.gridmind.entity.AlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<AlertEntity, String> {
    List<AlertEntity> findByAcknowledgedFalseOrderByCreatedAtDesc();
    List<AlertEntity> findAllByOrderByCreatedAtDesc();
    List<AlertEntity> findByTransformerIdOrderByCreatedAtDesc(String transformerId);
    List<AlertEntity> findByTransformerIdAndSeverityAndAcknowledgedFalse(String transformerId, GridStatus severity);
}
