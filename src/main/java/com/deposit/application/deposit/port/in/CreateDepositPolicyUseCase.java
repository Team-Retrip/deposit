package com.deposit.application.deposit.port.in;

import com.deposit.application.deposit.dto.command.CreateDepositPolicyCommand;
import com.deposit.application.deposit.dto.result.DepositPolicyResult;

public interface CreateDepositPolicyUseCase {
    DepositPolicyResult createPolicy(CreateDepositPolicyCommand command);
}
