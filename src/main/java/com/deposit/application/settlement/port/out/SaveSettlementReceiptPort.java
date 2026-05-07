package com.deposit.application.settlement.port.out;

import com.deposit.domain.settlement.SettlementReceipt;

public interface SaveSettlementReceiptPort {
    SettlementReceipt save(SettlementReceipt receipt);
}
