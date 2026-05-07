package com.deposit.application.settlement.port.in;

import com.deposit.application.settlement.dto.command.SplitSettlementCommand;
import com.deposit.application.settlement.dto.result.SplitSettlementResult;

public interface SplitSettlementUseCase {
    SplitSettlementResult split(SplitSettlementCommand command);
}
