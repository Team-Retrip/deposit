package com.deposit.application.settlement.port.in;

import com.deposit.application.settlement.dto.command.UpdateReceiptTotalCommand;
import com.deposit.application.settlement.dto.result.SettlementReceiptResult;

public interface UpdateReceiptTotalUseCase {
    SettlementReceiptResult updateTotal(UpdateReceiptTotalCommand command);
}
