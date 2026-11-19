package com.gridmind.repository;

import com.gridmind.entity.AlertRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRuleRepository extends JpaRepository<AlertRuleEntity, String> {
}
