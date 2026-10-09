package com.gridmind.repository;

import com.gridmind.entity.MaintenanceRecommendationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanceRecommendationRepository extends JpaRepository<MaintenanceRecommendationEntity, String> {
    List<MaintenanceRecommendationEntity> findByTransformerIdOrderByCreatedAtDesc(String transformerId);
    List<MaintenanceRecommendationEntity> findByStatusOrderByCreatedAtDesc(String status);
    List<MaintenanceRecommendationEntity> findAllByOrderByCreatedAtDesc();
}
