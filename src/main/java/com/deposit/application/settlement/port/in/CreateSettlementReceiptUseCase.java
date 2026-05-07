package com.deposit.application.settlement.port.in;

import com.deposit.application.settlement.dto.command.CreateSettlementReceiptCommand;
import com.deposit.application.settlement.dto.result.SettlementReceiptResult;

public interface CreateSettlementReceiptUseCase {
    SettlementReceiptResult create(CreateSettlementReceiptCommand command);
}
