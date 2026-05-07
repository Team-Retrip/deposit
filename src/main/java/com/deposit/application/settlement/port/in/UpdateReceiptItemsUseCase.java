package com.deposit.application.settlement.port.in;

import com.deposit.application.settlement.dto.command.UpdateReceiptItemsCommand;
import com.deposit.application.settlement.dto.result.SettlementReceiptResult;

public interface UpdateReceiptItemsUseCase {
    SettlementReceiptResult updateItems(UpdateReceiptItemsCommand command);
}
