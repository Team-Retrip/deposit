package com.deposit.application.deposit.port.out;

import com.deposit.domain.deposit.DepositPolicy;

public interface SaveDepositPolicyPort {
    DepositPolicy save(DepositPolicy policy);
}
