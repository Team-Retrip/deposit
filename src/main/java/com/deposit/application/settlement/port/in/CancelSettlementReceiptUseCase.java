package com.deposit.application.settlement.port.in;

import com.deposit.application.settlement.dto.result.SettlementReceiptResult;

public interface CancelSettlementReceiptUseCase {
    SettlementReceiptResult cancel(Long receiptId);
}
