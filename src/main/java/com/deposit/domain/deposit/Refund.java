package com.deposit.domain.deposit;

import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.deposit.vo.RefundStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 보증금 환불 내역
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Refund {

    private Long id;
    private Long depositId;
    private Money requestedAmount;
    private Money pgFeeAmount;
    private Money refundedAmount;
    private boolean feeDeducted;
    private RefundStatus status;
    private String pgRefundTransactionId;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;

    @Builder
    private Refund(Long id, Long depositId, Money requestedAmount, Money pgFeeAmount,
                   Money refundedAmount, boolean feeDeducted, RefundStatus status,
                   String pgRefundTransactionId, LocalDateTime requestedAt, LocalDateTime processedAt) {
        this.id = id;
        this.depositId = depositId;
        this.requestedAmount = requestedAmount;
        this.pgFeeAmount = pgFeeAmount;
        this.refundedAmount = refundedAmount;
        this.feeDeducted = feeDeducted;
        this.status = status;
        this.pgRefundTransactionId = pgRefundTransactionId;
        this.requestedAt = requestedAt;
        this.processedAt = processedAt;
    }

    public static Refund create(Deposit deposit) {
        boolean feeDeducted = !deposit.isWithinFreeRefundPeriod();
        Money pgFee = deposit.calculatePgFee();
        Money refundedAmount = deposit.calculateRefundAmount();

        return Refund.builder()
                .depositId(deposit.getId())
                .requestedAmount(deposit.getAmount())
                .pgFeeAmount(pgFee)
                .refundedAmount(refundedAmount)
                .feeDeducted(feeDeducted)
                .status(RefundStatus.REQUESTED)
                .requestedAt(LocalDateTime.now())
                .build();
    }

    public void complete(String pgRefundTransactionId) {
        this.pgRefundTransactionId = pgRefundTransactionId;
        this.status = RefundStatus.COMPLETED;
        this.processedAt = LocalDateTime.now();
    }

    public void fail() {
        this.status = RefundStatus.FAILED;
        this.processedAt = LocalDateTime.now();
    }
}
