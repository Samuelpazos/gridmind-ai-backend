package com.gridmind.repository;

import com.gridmind.entity.PredictionEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PredictionRepository extends JpaRepository<PredictionEntity, String> {
    List<PredictionEntity> findByTransformerIdOrderByEvaluatedAtDesc(String transformerId);
    List<PredictionEntity> findAllByOrderByEvaluatedAtDesc(Pageable pageable);

    default Optional<PredictionEntity> findLatestByTransformerId(String transformerId) {
        List<PredictionEntity> list = findByTransformerIdOrderByEvaluatedAtDesc(transformerId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}
