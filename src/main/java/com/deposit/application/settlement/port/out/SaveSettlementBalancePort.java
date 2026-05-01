package com.deposit.application.settlement.port.out;

import com.deposit.domain.settlement.SettlementBalance;

public interface SaveSettlementBalancePort {
    SettlementBalance save(SettlementBalance balance);
}
