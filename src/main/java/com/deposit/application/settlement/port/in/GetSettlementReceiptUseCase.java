package com.deposit.application.settlement.port.in;

import com.deposit.application.settlement.dto.result.SettlementReceiptResult;

import java.util.List;
import java.util.UUID;

public interface GetSettlementReceiptUseCase {
    SettlementReceiptResult getById(Long receiptId);
    List<SettlementReceiptResult> getByTrip(UUID tripId);
}
