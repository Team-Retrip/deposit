package com.deposit.application.deposit.port.in;

import com.deposit.application.deposit.dto.command.RefundDepositCommand;
import com.deposit.application.deposit.dto.result.RefundResult;

public interface RefundDepositUseCase {
    RefundResult refund(RefundDepositCommand command);
}
