package com.deposit.application.settlement.port.out;

import com.deposit.domain.settlement.SettlementReceipt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadSettlementReceiptPort {
    Optional<SettlementReceipt> findById(Long receiptId);
    List<SettlementReceipt> findAllByTripId(UUID tripId);
}
