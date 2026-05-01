package com.deposit.infrastructure.persistence.repository;

import com.deposit.infrastructure.persistence.entity.RefundJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefundJpaRepository extends JpaRepository<RefundJpaEntity, Long> {
    Optional<RefundJpaEntity> findByDepositId(Long depositId);
}
