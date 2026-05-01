package com.deposit.infrastructure.persistence.repository;

import com.deposit.infrastructure.persistence.entity.DepositPolicyJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DepositPolicyJpaRepository extends JpaRepository<DepositPolicyJpaEntity, Long> {
    Optional<DepositPolicyJpaEntity> findByTripId(UUID tripId);
    boolean existsByTripId(UUID tripId);
}
