package com.deposit.application.deposit.dto.result;

import com.deposit.domain.deposit.DepositPolicy;
import com.deposit.domain.deposit.vo.PolicyStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ConfirmTripResult {
    private UUID tripId;
    private PolicyStatus status;
    private int confirmedDeposits;
    private LocalDateTime tripStartAt;

    public static ConfirmTripResult of(DepositPolicy policy, int confirmedDeposits) {
        return ConfirmTripResult.builder()
                .tripId(policy.getTripId())
                .status(policy.getStatus())
                .confirmedDeposits(confirmedDeposits)
                .tripStartAt(policy.getTripStartAt())
                .build();
    }
}
