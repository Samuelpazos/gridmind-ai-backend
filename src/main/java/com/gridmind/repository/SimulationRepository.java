package com.gridmind.repository;

import com.gridmind.entity.SimulationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SimulationRepository extends JpaRepository<SimulationEntity, String> {
    List<SimulationEntity> findByTransformerIdOrderByExecutedAtDesc(String transformerId);
    List<SimulationEntity> findAllByOrderByExecutedAtDesc();
}
