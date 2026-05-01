package com.deposit.infrastructure.settlement.repository;

import com.deposit.infrastructure.settlement.entity.SettlementJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SettlementJpaRepository extends JpaRepository<SettlementJpaEntity, Long> {
    List<SettlementJpaEntity> findAllByTripId(UUID tripId);
}
