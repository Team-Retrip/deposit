package com.deposit.application.deposit.dto.result;

import com.deposit.domain.deposit.DepositPolicy;
import com.deposit.domain.deposit.vo.PolicyStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class DepositPolicyResult {
    private Long id;
    private UUID tripId;
    private Long organizerId;
    private BigDecimal depositAmount;
    private PolicyStatus status;
    private LocalDateTime createdAt;

    public static DepositPolicyResult from(DepositPolicy policy) {
        return DepositPolicyResult.builder()
                .id(policy.getId())
                .tripId(policy.getTripId())
                .organizerId(policy.getOrganizerId())
                .depositAmount(policy.getDepositAmount().getAmount())
                .status(policy.getStatus())
                .createdAt(policy.getCreatedAt())
                .build();
    }
}
