package com.deposit.application.settlement.port.out;

import com.deposit.domain.settlement.Settlement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadSettlementPort {
    Optional<Settlement> findById(Long settlementId);
    List<Settlement> findAllByTripId(UUID tripId);
}
