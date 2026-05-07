package com.deposit.infrastructure.persistence.repository;

import com.deposit.domain.deposit.vo.PolicyStatus;
import com.deposit.infrastructure.persistence.entity.DepositPolicyJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepositPolicyJpaRepository extends JpaRepository<DepositPolicyJpaEntity, Long> {
    Optional<DepositPolicyJpaEntity> findByTripId(UUID tripId);
    boolean existsByTripId(UUID tripId);

    /** ACTIVE 상태이고 tripStartAt이 기준 시각보다 이전인 정책 (자동 환불 대상) */
    List<DepositPolicyJpaEntity> findByStatusAndTripStartAtBefore(PolicyStatus status, LocalDateTime dateTime);
}
