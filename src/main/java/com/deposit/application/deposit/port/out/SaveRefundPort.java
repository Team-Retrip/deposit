package com.deposit.application.deposit.port.out;

import com.deposit.domain.deposit.Refund;

import java.util.Optional;

public interface SaveRefundPort {
    Refund save(Refund refund);
    Optional<Refund> findByDepositId(Long depositId);
}
