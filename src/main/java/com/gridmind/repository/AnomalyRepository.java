package com.gridmind.repository;

import com.gridmind.entity.AnomalyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.ZonedDateTime;

@Repository
public interface AnomalyRepository extends JpaRepository<AnomalyEntity, String> {
    List<AnomalyEntity> findByTransformerIdOrderByDetectedAtDesc(String transformerId);

    @Modifying
    @Query("DELETE FROM AnomalyEntity a WHERE a.detectedAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") ZonedDateTime cutoff);
}
