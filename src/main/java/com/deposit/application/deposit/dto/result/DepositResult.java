package com.deposit.application.deposit.dto.result;

import com.deposit.domain.deposit.Deposit;
import com.deposit.domain.deposit.vo.DepositStatus;
import com.deposit.domain.deposit.vo.PaymentMethod;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class DepositResult {
    private Long id;
    private UUID tripId;
    private Long userId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private DepositStatus status;
    private LocalDateTime paidAt;
    private LocalDateTime refundDeadline;

    /** 30일 안내 메시지 (납부 완료 시 포함) */
    private String refundNotice;

    public static DepositResult from(Deposit deposit) {
        return DepositResult.builder()
                .id(deposit.getId())
                .tripId(deposit.getTripId())
                .userId(deposit.getUserId())
                .amount(deposit.getAmount().getAmount())
                .paymentMethod(deposit.getPaymentMethod())
                .status(deposit.getStatus())
                .paidAt(deposit.getPaidAt())
                .refundDeadline(deposit.getRefundDeadline())
                .refundNotice(buildRefundNotice(deposit))
                .build();
    }

    private static String buildRefundNotice(Deposit deposit) {
        if (deposit.getRefundDeadline() == null || deposit.getPgFeeRate() == null) {
            return null;
        }
        BigDecimal feePercent = deposit.getPgFeeRate().multiply(java.math.BigDecimal.valueOf(100));
        return String.format(
                "보증금 환불은 결제일로부터 30일 이내(%s까지)에 요청해야 합니다. " +
                "30일 이후 환불 시 PG 결제 수수료 %.1f%%가 공제된 금액이 환불됩니다.",
                deposit.getRefundDeadline().toLocalDate(),
                feePercent
        );
    }
}
