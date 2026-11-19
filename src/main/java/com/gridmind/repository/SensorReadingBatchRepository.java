package com.gridmind.repository;

import com.gridmind.entity.SensorReadingBatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SensorReadingBatchRepository extends JpaRepository<SensorReadingBatchEntity, String> {
}
