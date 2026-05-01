package com.deposit.application.deposit.port.in;

import com.deposit.application.deposit.dto.command.RequestDepositCommand;
import com.deposit.application.deposit.dto.result.DepositResult;

import java.util.List;

public interface RequestDepositUseCase {
    List<DepositResult> requestDeposits(RequestDepositCommand command);
}
