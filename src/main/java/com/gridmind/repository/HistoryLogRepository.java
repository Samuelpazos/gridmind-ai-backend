package com.gridmind.repository;

import com.gridmind.entity.HistoryLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoryLogRepository extends JpaRepository<HistoryLogEntity, String> {
    List<HistoryLogEntity> findAllByOrderByTimestampDesc();
    List<HistoryLogEntity> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, String entityId);
}
