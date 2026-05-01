package com.deposit.application.settlement.port.in;

import com.deposit.application.settlement.dto.command.UpdateSettlementCommand;
import com.deposit.application.settlement.dto.result.SettlementResult;

public interface UpdateSettlementUseCase {
    SettlementResult update(UpdateSettlementCommand command);
}
