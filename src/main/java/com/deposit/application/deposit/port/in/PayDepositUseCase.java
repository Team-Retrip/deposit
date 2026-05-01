package com.deposit.application.deposit.port.in;

import com.deposit.application.deposit.dto.command.PayDepositCommand;
import com.deposit.application.deposit.dto.result.DepositResult;

public interface PayDepositUseCase {
    DepositResult pay(PayDepositCommand command);
}
