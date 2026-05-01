package com.deposit.application.settlement.port.in;

import com.deposit.application.settlement.dto.result.SettlementResult;

import java.util.List;
import java.util.UUID;

public interface GetTripSettlementsUseCase {
    List<SettlementResult> getByTrip(UUID tripId);
}
