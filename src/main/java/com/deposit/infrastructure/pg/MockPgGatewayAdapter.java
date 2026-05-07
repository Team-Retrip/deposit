package com.deposit.infrastructure.pg;

import com.deposit.application.deposit.port.out.PgGatewayPort;
import com.deposit.domain.deposit.vo.PaymentMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * PG사 연동 Mock 구현체
 * 실제 환경에서는 토스페이먼츠, KG이니시스 등 PG사 SDK로 교체
 */
@Slf4j
@Component
public class MockPgGatewayAdapter implements PgGatewayPort {

    @Value("${pg.fee-rate.card}")
    private BigDecimal cardFeeRate;

    @Value("${pg.fee-rate.bank-transfer}")
    private BigDecimal bankTransferFeeRate;

    @Override
    public PayResult pay(PayCommand command) {
        String pgName = command.pgProvider() != null ? command.pgProvider().name() : "DEFAULT";
        log.info("[PG:{}] 결제 요청 - userId={}, amount={}, method={}",
                pgName, command.userId(), command.amount(), command.paymentMethod());

        String pgTransactionId = "PG-PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("[PG:{}] 결제 승인 완료 - pgTransactionId={}", pgName, pgTransactionId);

        return new PayResult(true, pgTransactionId, null);
    }

    @Override
    public RefundResult refund(RefundCommand command) {
        log.info("[PG] 환불 요청 - pgTransactionId={}, amount={}, reason={}",
                command.pgTransactionId(), command.refundAmount(), command.reason());

        String pgRefundTransactionId = "PG-REFUND-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("[PG] 환불 완료 - pgRefundTransactionId={}", pgRefundTransactionId);

        return new RefundResult(true, pgRefundTransactionId, null);
    }

    @Override
    public BigDecimal getFeeRate(PaymentMethod paymentMethod) {
        return switch (paymentMethod) {
            case CARD -> cardFeeRate;
            case BANK_TRANSFER -> bankTransferFeeRate;
        };
    }
}
