package com.gridmind.repository;

import com.gridmind.entity.TransformerEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransformerRepository extends JpaRepository<TransformerEntity, String> {
    Optional<TransformerEntity> findByCode(String code);
    boolean existsByCode(String code);
    List<TransformerEntity> findBySubstationId(String substationId);
    Optional<TransformerEntity> findByIdAndArchivedAtIsNull(String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TransformerEntity t WHERE t.id = :id AND t.archivedAt IS NULL")
    Optional<TransformerEntity> lockActiveById(@Param("id") String id);
    Optional<TransformerEntity> findByIdAndArchivedAtIsNotNull(String id);
    List<TransformerEntity> findAllByArchivedAtIsNull();
    List<TransformerEntity> findAllByArchivedAtIsNotNullOrderByArchivedAtDesc();
    long countByArchivedAtIsNull();

    @Query("SELECT t FROM TransformerEntity t WHERE t.archivedAt IS NULL ORDER BY t.priorityScore DESC")
    List<TransformerEntity> findAllOrderedByPriority();
}
