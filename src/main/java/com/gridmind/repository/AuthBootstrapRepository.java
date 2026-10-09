package com.gridmind.repository;

import com.gridmind.entity.AuthBootstrapEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AuthBootstrapRepository extends JpaRepository<AuthBootstrapEntity, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT bootstrap FROM AuthBootstrapEntity bootstrap WHERE bootstrap.id = 1")
    Optional<AuthBootstrapEntity> lockBootstrapRecord();
}
