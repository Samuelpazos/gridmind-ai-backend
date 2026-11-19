package com.gridmind.repository;

import com.gridmind.entity.SensorTypeCatalogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SensorTypeCatalogRepository extends JpaRepository<SensorTypeCatalogEntity, String> {
    java.util.Optional<SensorTypeCatalogEntity> findByCodeAndActiveTrue(String code);
}
