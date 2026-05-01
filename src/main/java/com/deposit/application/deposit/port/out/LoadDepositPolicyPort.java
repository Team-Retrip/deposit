package com.deposit.application.deposit.port.out;

import com.deposit.domain.deposit.DepositPolicy;

import java.util.Optional;
import java.util.UUID;

public interface LoadDepositPolicyPort {
    Optional<DepositPolicy> findByTripId(UUID tripId);
    Optional<DepositPolicy> findById(Long policyId);
}
