package com.gridmind.repository;

import com.gridmind.entity.CommandLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommandLogRepository extends JpaRepository<CommandLogEntity, String> {
    List<CommandLogEntity> findAllByOrderByExecutedAtDesc();
}
