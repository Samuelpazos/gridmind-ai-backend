package com.gridmind.repository;

import com.gridmind.entity.TransformerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransformerRepository extends JpaRepository<TransformerEntity, String> {
    Optional<TransformerEntity> findByCode(String code);
    List<TransformerEntity> findBySubstationId(String substationId);

    @Query("SELECT t FROM TransformerEntity t ORDER BY t.priorityScore DESC")
    List<TransformerEntity> findAllOrderedByPriority();
}
