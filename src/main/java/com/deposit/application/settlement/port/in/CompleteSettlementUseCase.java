package com.deposit.application.settlement.port.in;

import com.deposit.application.settlement.dto.result.SettlementResult;

public interface CompleteSettlementUseCase {
    SettlementResult complete(Long settlementId);
}
