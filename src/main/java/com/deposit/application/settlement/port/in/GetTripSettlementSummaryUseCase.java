package com.deposit.application.settlement.port.in;

import com.deposit.application.settlement.dto.result.SettlementSummaryResult;

import java.util.UUID;

public interface GetTripSettlementSummaryUseCase {
    SettlementSummaryResult getSummary(UUID tripId);
}
