package com.gridmind.repository;

import com.gridmind.entity.SensorCalibrationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SensorCalibrationRepository extends JpaRepository<SensorCalibrationEntity, String> {
}
