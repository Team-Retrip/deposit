package com.deposit.infrastructure.persistence.repository;

import com.deposit.infrastructure.persistence.entity.DepositJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepositJpaRepository extends JpaRepository<DepositJpaEntity, Long> {
    Optional<DepositJpaEntity> findByTripIdAndUserId(UUID tripId, Long userId);
    List<DepositJpaEntity> findAllByTripId(UUID tripId);
    boolean existsByTripIdAndUserId(UUID tripId, Long userId);
}
