package com.deposit.application.deposit.port.out;

import com.deposit.domain.deposit.Deposit;

public interface SaveDepositPort {
    Deposit save(Deposit deposit);
}
