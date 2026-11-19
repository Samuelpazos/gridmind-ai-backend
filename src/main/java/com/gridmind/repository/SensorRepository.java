package com.gridmind.repository;

import com.gridmind.domain.SensorType;
import com.gridmind.domain.TransformerAssetType;
import com.gridmind.entity.SensorEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SensorRepository extends JpaRepository<SensorEntity, String> {
    Optional<SensorEntity> findByCode(String code);
    Optional<SensorEntity> findByCodeAndTransformerArchivedAtIsNull(String code);
    boolean existsByCode(String code);
    List<SensorEntity> findByTransformerId(String transformerId);
    Optional<SensorEntity> findByTransformerIdAndType(String transformerId, SensorType type);

    @EntityGraph(attributePaths = "transformer")
    List<SensorEntity> findAllByTransformerAssetTypeAndTransformerArchivedAtIsNull(TransformerAssetType assetType);

    long countByTransformerArchivedAtIsNull();
}
