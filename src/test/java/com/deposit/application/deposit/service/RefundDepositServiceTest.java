package com.deposit.application.deposit.service;

import com.deposit.application.deposit.dto.command.RefundDepositCommand;
import com.deposit.application.deposit.dto.result.RefundResult;
import com.deposit.application.deposit.port.out.LoadDepositPort;
import com.deposit.application.deposit.port.out.PgGatewayPort;
import com.deposit.application.deposit.port.out.SaveDepositPort;
import com.deposit.application.deposit.port.out.SaveRefundPort;
import com.deposit.common.exception.BusinessException;
import com.deposit.common.exception.ErrorCode;
import com.deposit.domain.deposit.Deposit;
import com.deposit.domain.deposit.vo.DepositStatus;
import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.deposit.vo.PaymentMethod;
import com.deposit.domain.deposit.vo.RefundStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefundDepositService")
class RefundDepositServiceTest {

    private static final UUID TRIP_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

    @Mock LoadDepositPort loadDepositPort;
    @Mock SaveDepositPort saveDepositPort;
    @Mock SaveRefundPort saveRefundPort;
    @Mock PgGatewayPort pgGatewayPort;

    @InjectMocks
    RefundDepositService sut;

    @Test
    @DisplayName("30일 이내 환불 → 수수료 없이 전액 환불")
    void refund_withinPeriod_fullRefund() {
        Deposit paid = paidDeposit(LocalDateTime.now().plusDays(10));
        given(loadDepositPort.findById(1L)).willReturn(Optional.of(paid));
        given(pgGatewayPort.refund(any())).willReturn(
                new PgGatewayPort.RefundResult(true, "PG-REFUND-001", null));
        given(saveDepositPort.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(saveRefundPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        RefundResult result = sut.refund(RefundDepositCommand.builder()
                .depositId(1L).userId(100L).build());

        assertThat(result.isFeeDeducted()).isFalse();
        assertThat(result.getPgFeeAmount()).isEqualByComparingTo("0");
        assertThat(result.getRefundedAmount()).isEqualByComparingTo("50000");
        assertThat(result.getStatus()).isEqualTo(RefundStatus.COMPLETED);
    }

    @Test
    @DisplayName("30일 초과 환불 → 카드 수수료 공제(3.5%)")
    void refund_afterPeriod_feeDeducted() {
        Deposit expired = paidDeposit(LocalDateTime.now().minusDays(1));
        given(loadDepositPort.findById(1L)).willReturn(Optional.of(expired));
        given(pgGatewayPort.refund(any())).willReturn(
                new PgGatewayPort.RefundResult(true, "PG-REFUND-002", null));
        given(saveDepositPort.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(saveRefundPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        RefundResult result = sut.refund(RefundDepositCommand.builder()
                .depositId(1L).userId(100L).build());

        assertThat(result.isFeeDeducted()).isTrue();
        assertThat(result.getPgFeeAmount()).isEqualByComparingTo("1750"); // 50000 * 3.5%
        assertThat(result.getRefundedAmount()).isEqualByComparingTo("48250");
    }

    @Test
    @DisplayName("PG 환불 실패 → 환불 내역 FAILED 저장 후 예외")
    void refund_pgFailed_savesFailedAndThrows() {
        Deposit paid = paidDeposit(LocalDateTime.now().plusDays(10));
        given(loadDepositPort.findById(1L)).willReturn(Optional.of(paid));
        given(pgGatewayPort.refund(any())).willReturn(
                new PgGatewayPort.RefundResult(false, null, "환불 불가"));
        given(saveRefundPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> sut.refund(RefundDepositCommand.builder()
                .depositId(1L).userId(100L).build()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PG_REFUND_FAILED);

        then(saveRefundPort).should().save(argThat(r -> r.getStatus() == RefundStatus.FAILED));
    }

    @Test
    @DisplayName("미납 보증금 환불 요청 시 예외")
    void refund_notPaid_throws() {
        Deposit pending = Deposit.create(TRIP_ID, 100L, Money.wons(50000));
        given(loadDepositPort.findById(1L)).willReturn(Optional.of(pending));

        assertThatThrownBy(() -> sut.refund(RefundDepositCommand.builder()
                .depositId(1L).userId(100L).build()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DEPOSIT_NOT_PAID);
    }

    private Deposit paidDeposit(LocalDateTime refundDeadline) {
        return Deposit.builder()
                .id(1L).tripId(TRIP_ID).userId(100L)
                .amount(Money.wons(50000))
                .status(DepositStatus.PAID)
                .pgTransactionId("PG-TXN-001")
                .pgFeeRate(new BigDecimal("0.035"))
                .paymentMethod(PaymentMethod.CARD)
                .paidAt(LocalDateTime.now().minusDays(5))
                .refundDeadline(refundDeadline)
                .createdAt(LocalDateTime.now().minusDays(5))
                .build();
    }
}
