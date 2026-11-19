package com.gridmind.repository;

import com.gridmind.entity.CommandSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommandSnapshotRepository extends JpaRepository<CommandSnapshotEntity, String> {
    Optional<CommandSnapshotEntity> findFirstByCommandLog_ExecutedByAndConsumedAtIsNullOrderByCreatedAtDesc(
            String executedBy);
}
