package com.deposit.domain.deposit;

import com.deposit.domain.deposit.vo.DepositStatus;
import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.deposit.vo.PaymentMethod;
import com.deposit.domain.deposit.vo.RefundStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Refund 도메인")
class RefundTest {

    private static final BigDecimal CARD_FEE_RATE = new BigDecimal("0.035");
    private static final UUID TRIP_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

    @Test
    @DisplayName("30일 이내 환불 → 수수료 0, 전액 환불")
    void create_withinPeriod_noFee() {
        Deposit deposit = paidDeposit(LocalDateTime.now().plusDays(29));
        Refund refund = Refund.create(deposit);

        assertThat(refund.isFeeDeducted()).isFalse();
        assertThat(refund.getPgFeeAmount()).isEqualTo(Money.ZERO);
        assertThat(refund.getRefundedAmount()).isEqualTo(Money.wons(50000));
        assertThat(refund.getStatus()).isEqualTo(RefundStatus.REQUESTED);
    }

    @Test
    @DisplayName("30일 초과 환불 → 카드 수수료(3.5%) 공제")
    void create_afterPeriod_feeDeducted() {
        Deposit deposit = paidDeposit(LocalDateTime.now().minusDays(1));
        Refund refund = Refund.create(deposit);

        assertThat(refund.isFeeDeducted()).isTrue();
        assertThat(refund.getPgFeeAmount().getAmount()).isEqualByComparingTo("1750");
        assertThat(refund.getRefundedAmount().getAmount()).isEqualByComparingTo("48250");
    }

    @Test
    @DisplayName("환불 완료 처리 → COMPLETED 상태, pgRefundTransactionId 설정")
    void complete_setsStatus() {
        Deposit deposit = paidDeposit(LocalDateTime.now().plusDays(10));
        Refund refund = Refund.create(deposit);
        refund.complete("PG-REFUND-001");

        assertThat(refund.getStatus()).isEqualTo(RefundStatus.COMPLETED);
        assertThat(refund.getPgRefundTransactionId()).isEqualTo("PG-REFUND-001");
        assertThat(refund.getProcessedAt()).isNotNull();
    }

    @Test
    @DisplayName("환불 실패 처리 → FAILED 상태")
    void fail_setsStatus() {
        Deposit deposit = paidDeposit(LocalDateTime.now().plusDays(10));
        Refund refund = Refund.create(deposit);
        refund.fail();

        assertThat(refund.getStatus()).isEqualTo(RefundStatus.FAILED);
    }

    private Deposit paidDeposit(LocalDateTime refundDeadline) {
        return Deposit.builder()
                .id(1L)
                .tripId(TRIP_ID)
                .userId(100L)
                .amount(Money.wons(50000))
                .status(DepositStatus.PAID)
                .pgTransactionId("PG-TXN-001")
                .pgFeeRate(CARD_FEE_RATE)
                .paymentMethod(PaymentMethod.CARD)
                .paidAt(LocalDateTime.now().minusDays(31).plusDays(1))
                .refundDeadline(refundDeadline)
                .createdAt(LocalDateTime.now().minusDays(31))
                .build();
    }
}
