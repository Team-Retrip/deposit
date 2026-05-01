package com.deposit.application.settlement.port.out;

import com.deposit.domain.settlement.SettlementBalance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadSettlementBalancePort {
    Optional<SettlementBalance> findByTripIdAndPayerIdAndDebtorId(UUID tripId, Long payerId, Long debtorId);
    List<SettlementBalance> findAllByTripId(UUID tripId);
    List<SettlementBalance> findAllByTripIdAndDebtorId(UUID tripId, Long debtorId);
}
