package com.deposit.application.deposit.port.out;

import com.deposit.domain.deposit.DepositPolicy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadDepositPolicyPort {
    Optional<DepositPolicy> findByTripId(UUID tripId);
    Optional<DepositPolicy> findById(Long policyId);
    /** ACTIVE 상태이면서 tripStartAt이 기준 시각보다 이전인 정책(자동 환불 대상) */
    List<DepositPolicy> findAllActiveExpiredBefore(LocalDateTime dateTime);
}
