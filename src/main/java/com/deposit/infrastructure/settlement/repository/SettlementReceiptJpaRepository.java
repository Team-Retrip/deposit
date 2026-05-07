package com.deposit.infrastructure.settlement.repository;

import com.deposit.infrastructure.settlement.entity.SettlementReceiptJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SettlementReceiptJpaRepository extends JpaRepository<SettlementReceiptJpaEntity, Long> {
    List<SettlementReceiptJpaEntity> findAllByTripId(UUID tripId);
}
