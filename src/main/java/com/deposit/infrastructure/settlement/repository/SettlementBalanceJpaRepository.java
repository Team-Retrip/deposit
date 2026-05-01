package com.deposit.infrastructure.settlement.repository;

import com.deposit.infrastructure.settlement.entity.SettlementBalanceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SettlementBalanceJpaRepository extends JpaRepository<SettlementBalanceJpaEntity, Long> {
    Optional<SettlementBalanceJpaEntity> findByTripIdAndPayerIdAndDebtorId(UUID tripId, Long payerId, Long debtorId);
    List<SettlementBalanceJpaEntity> findAllByTripId(UUID tripId);
    List<SettlementBalanceJpaEntity> findAllByTripIdAndDebtorId(UUID tripId, Long debtorId);
}
