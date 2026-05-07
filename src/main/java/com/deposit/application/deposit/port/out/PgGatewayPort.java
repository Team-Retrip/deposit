package com.deposit.application.deposit.port.out;

import com.deposit.domain.deposit.vo.PaymentMethod;
import com.deposit.domain.deposit.vo.PgProvider;

import java.math.BigDecimal;

public interface PgGatewayPort {

    record PayCommand(
            String orderId,
            Long userId,
            BigDecimal amount,
            PaymentMethod paymentMethod,
            String orderName,
            PgProvider pgProvider
    ) {}

    record PayResult(
            boolean success,
            String pgTransactionId,
            String failureReason
    ) {}

    record RefundCommand(
            String pgTransactionId,
            BigDecimal refundAmount,
            String reason
    ) {}

    record RefundResult(
            boolean success,
            String pgRefundTransactionId,
            String failureReason
    ) {}

    PayResult pay(PayCommand command);
    RefundResult refund(RefundCommand command);

    /** 결제 수단별 PG 수수료율 반환 */
    BigDecimal getFeeRate(PaymentMethod paymentMethod);
}
