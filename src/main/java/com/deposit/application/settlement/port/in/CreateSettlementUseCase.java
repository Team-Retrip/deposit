package com.deposit.application.settlement.port.in;

import com.deposit.application.settlement.dto.command.CreateSettlementCommand;
import com.deposit.application.settlement.dto.result.SettlementResult;

public interface CreateSettlementUseCase {
    SettlementResult create(CreateSettlementCommand command);
}
