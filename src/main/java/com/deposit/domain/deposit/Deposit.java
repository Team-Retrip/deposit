package com.deposit.domain.deposit;

import com.deposit.common.exception.BusinessException;
import com.deposit.common.exception.ErrorCode;
import com.deposit.domain.deposit.vo.DepositStatus;
import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.deposit.vo.PaymentMethod;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 참가자별 보증금 납부 건
 * 납부 후 30일 이내 환불 요청 시 수수료 없음, 이후 PG 수수료 공제
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Deposit {

    private static final int REFUND_FREE_DAYS = 30;
    public static final BigDecimal LATE_REFUND_FEE_RATE = new BigDecimal("0.035");

    private Long id;
    private UUID tripId;
    private Long userId;
    private Money amount;
    private PaymentMethod paymentMethod;
    private DepositStatus status;
    private String pgTransactionId;
    private BigDecimal pgFeeRate;
    private LocalDateTime paidAt;
    private LocalDateTime refundDeadline;
    private LocalDateTime createdAt;

    @Builder
    private Deposit(Long id, UUID tripId, Long userId, Money amount,
                    PaymentMethod paymentMethod, DepositStatus status,
                    String pgTransactionId, BigDecimal pgFeeRate,
                    LocalDateTime paidAt, LocalDateTime refundDeadline,
                    LocalDateTime createdAt) {
        this.id = id;
        this.tripId = tripId;
        this.userId = userId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.pgTransactionId = pgTransactionId;
        this.pgFeeRate = pgFeeRate;
        this.paidAt = paidAt;
        this.refundDeadline = refundDeadline;
        this.createdAt = createdAt;
    }

    public static Deposit create(UUID tripId, Long userId, Money amount) {
        return Deposit.builder()
                .tripId(tripId)
                .userId(userId)
                .amount(amount)
                .status(DepositStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * PG 결제 승인 처리
     * 결제일로부터 30일이 환불 무료 기간
     */
    public void pay(String pgTransactionId, BigDecimal pgFeeRate, PaymentMethod paymentMethod) {
        if (this.status != DepositStatus.PENDING) {
            throw new BusinessException(ErrorCode.DEPOSIT_ALREADY_PAID);
        }
        this.pgTransactionId = pgTransactionId;
        this.pgFeeRate = pgFeeRate;
        this.paymentMethod = paymentMethod;
        this.status = DepositStatus.PAID;
        this.paidAt = LocalDateTime.now();
        this.refundDeadline = this.paidAt.plusDays(REFUND_FREE_DAYS);
    }

    /**
     * 환불 요청 상태로 전환
     */
    public void requestRefund() {
        if (this.status == DepositStatus.REFUNDED) {
            throw new BusinessException(ErrorCode.DEPOSIT_ALREADY_REFUNDED);
        }
        if (this.status == DepositStatus.REFUND_REQUESTED) {
            throw new BusinessException(ErrorCode.DEPOSIT_REFUND_IN_PROGRESS);
        }
        if (this.status != DepositStatus.PAID) {
            throw new BusinessException(ErrorCode.DEPOSIT_NOT_PAID);
        }
        this.status = DepositStatus.REFUND_REQUESTED;
    }

    /**
     * 환불 완료 처리
     */
    public void completeRefund() {
        this.status = DepositStatus.REFUNDED;
    }

    /**
     * 무료 환불 기간(30일) 이내 여부
     */
    public boolean isWithinFreeRefundPeriod() {
        if (refundDeadline == null) return false;
        return LocalDateTime.now().isBefore(refundDeadline);
    }

    /**
     * PG 수수료를 고려한 실제 환불 금액 계산
     * 30일 이내: 전액 환불
     * 30일 이후: 보증금 - PG 수수료
     */
    public Money calculateRefundAmount() {
        if (isWithinFreeRefundPeriod()) {
            return this.amount;
        }
        Money fee = this.amount.multiply(pgFeeRate);
        return this.amount.subtract(fee);
    }

    /**
     * PG 수수료 금액
     */
    public Money calculatePgFee() {
        if (isWithinFreeRefundPeriod()) {
            return Money.ZERO;
        }
        return this.amount.multiply(pgFeeRate);
    }

    public boolean isPaid() {
        return this.status == DepositStatus.PAID
                || this.status == DepositStatus.REFUND_REQUESTED
                || this.status == DepositStatus.REFUNDED;
    }
}
