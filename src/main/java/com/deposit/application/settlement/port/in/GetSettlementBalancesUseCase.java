package com.deposit.application.settlement.port.in;

import com.deposit.application.settlement.dto.result.SettlementBalanceResult;

import java.util.List;
import java.util.UUID;

public interface GetSettlementBalancesUseCase {
    List<SettlementBalanceResult> getBalancesByTrip(UUID tripId);
    List<SettlementBalanceResult> getBalancesByTripAndDebtor(UUID tripId, Long debtorId);
}
