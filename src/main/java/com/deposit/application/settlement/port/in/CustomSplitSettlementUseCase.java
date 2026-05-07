package com.deposit.application.settlement.port.in;

import com.deposit.application.settlement.dto.command.CustomSplitSettlementCommand;
import com.deposit.application.settlement.dto.result.SplitSettlementResult;

public interface CustomSplitSettlementUseCase {
    SplitSettlementResult split(CustomSplitSettlementCommand command);
}
