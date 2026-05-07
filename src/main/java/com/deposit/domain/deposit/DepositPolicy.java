package com.deposit.domain.deposit;

import com.deposit.common.exception.BusinessException;
import com.deposit.common.exception.ErrorCode;
import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.deposit.vo.PgProvider;
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
    private PgProvider pgProvider;
    private LocalDateTime tripStartAt;
    private LocalDateTime createdAt;

    @Builder
    private DepositPolicy(Long id, UUID tripId, Long organizerId, Money depositAmount,
                          PolicyStatus status, PgProvider pgProvider,
                          LocalDateTime tripStartAt, LocalDateTime createdAt) {
        this.id = id;
        this.tripId = tripId;
        this.organizerId = organizerId;
        this.depositAmount = depositAmount;
        this.status = status;
        this.pgProvider = pgProvider;
        this.tripStartAt = tripStartAt;
        this.createdAt = createdAt;
    }

    public static DepositPolicy create(UUID tripId, Long organizerId, Money depositAmount,
                                       PgProvider pgProvider, LocalDateTime tripStartAt) {
        return DepositPolicy.builder()
                .tripId(tripId)
                .organizerId(organizerId)
                .depositAmount(depositAmount)
                .status(PolicyStatus.ACTIVE)
                .pgProvider(pgProvider)
                .tripStartAt(tripStartAt)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public Deposit issueDeposit(Long userId) {
        return Deposit.create(this.tripId, userId, this.depositAmount);
    }

    /** 여행 확정: 전원 납부 완료 시 방장이 확정 처리 */
    public void confirm() {
        if (isConfirmed()) {
            throw new BusinessException(ErrorCode.TRIP_ALREADY_CONFIRMED);
        }
        this.status = PolicyStatus.CONFIRMED;
    }

    /** 자동 취소: 여행 시작 시간 초과 + 미확정 시 자동 환불 후 취소 */
    public void cancel() {
        this.status = PolicyStatus.CANCELLED;
    }

    public void deactivate() {
        this.status = PolicyStatus.INACTIVE;
    }

    public boolean isActive() {
        return this.status == PolicyStatus.ACTIVE;
    }

    public boolean isConfirmed() {
        return this.status == PolicyStatus.CONFIRMED;
    }

    public boolean isCancelled() {
        return this.status == PolicyStatus.CANCELLED;
    }

    /** 여행 시작 시간이 지났는데도 미확정 상태인지 */
    public boolean isTripStartExpiredWithoutConfirmation() {
        if (tripStartAt == null) return false;
        return LocalDateTime.now().isAfter(tripStartAt) && isActive();
    }
}
