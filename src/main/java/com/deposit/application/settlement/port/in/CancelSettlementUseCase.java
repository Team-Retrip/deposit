package com.deposit.application.settlement.port.in;

import com.deposit.application.settlement.dto.command.CancelSettlementCommand;
import com.deposit.application.settlement.dto.result.SettlementResult;

public interface CancelSettlementUseCase {
    SettlementResult cancel(CancelSettlementCommand command);
}
