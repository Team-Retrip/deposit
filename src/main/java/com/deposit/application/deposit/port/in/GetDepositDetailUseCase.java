package com.deposit.application.deposit.port.in;

import com.deposit.application.deposit.dto.result.DepositResult;

public interface GetDepositDetailUseCase {
    DepositResult getDeposit(Long depositId);
}
