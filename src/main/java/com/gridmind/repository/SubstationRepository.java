package com.gridmind.repository;

import com.gridmind.entity.SubstationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubstationRepository extends JpaRepository<SubstationEntity, String> {
    Optional<SubstationEntity> findByCode(String code);
}
