package com.deposit.domain.deposit;

import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.deposit.vo.PolicyStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 여행 보증금 정책 - 방장이 여행에 설정하는 보증금 규칙
 * Trip은 외부 도메인이므로 tripId만 참조
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DepositPolicy {

    private Long id;
    private UUID tripId;
    private Long organizerId;
    private Money depositAmount;
    private PolicyStatus status;
    private LocalDateTime createdAt;

    @Builder
    private DepositPolicy(Long id, UUID tripId, Long organizerId, Money depositAmount,
                          PolicyStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.tripId = tripId;
        this.organizerId = organizerId;
        this.depositAmount = depositAmount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static DepositPolicy create(UUID tripId, Long organizerId, Money depositAmount) {
        return DepositPolicy.builder()
                .tripId(tripId)
                .organizerId(organizerId)
                .depositAmount(depositAmount)
                .status(PolicyStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public Deposit issueDeposit(Long userId) {
        return Deposit.create(this.tripId, userId, this.depositAmount);
    }

    public void deactivate() {
        this.status = PolicyStatus.INACTIVE;
    }

    public boolean isActive() {
        return this.status == PolicyStatus.ACTIVE;
    }
}
