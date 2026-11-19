package com.gridmind.repository;

import com.gridmind.entity.SensorReadingEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.ZonedDateTime;

@Repository
public interface SensorReadingRepository extends JpaRepository<SensorReadingEntity, String> {
    List<SensorReadingEntity> findBySensorIdOrderByTimestampDesc(String sensorId, Pageable pageable);

    @Query("SELECT DISTINCT r.source FROM SensorReadingEntity r WHERE r.timestamp BETWEEN :since AND :until")
    List<String> findDistinctSourcesBetween(
            @Param("since") ZonedDateTime since,
            @Param("until") ZonedDateTime until);

    Optional<SensorReadingEntity> findFirstBySourceOrderByTimestampDesc(String source);

    @Query("SELECT r FROM SensorReadingEntity r WHERE r.sensor.id = :sensorId ORDER BY r.timestamp DESC")
    List<SensorReadingEntity> findLatestBySensorId(String sensorId, Pageable pageable);

    default Optional<SensorReadingEntity> findLatestBySensorId(String sensorId) {
        List<SensorReadingEntity> list = findLatestBySensorId(sensorId, org.springframework.data.domain.PageRequest.of(0, 1));
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Modifying
    @Query("DELETE FROM SensorReadingEntity r WHERE r.timestamp < :cutoff")
    int deleteOlderThan(@Param("cutoff") ZonedDateTime cutoff);
}
