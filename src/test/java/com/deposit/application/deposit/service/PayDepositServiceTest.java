package com.deposit.application.deposit.service;

import com.deposit.application.deposit.dto.command.PayDepositCommand;
import com.deposit.application.deposit.dto.result.DepositResult;
import com.deposit.application.deposit.port.out.LoadDepositPort;
import com.deposit.application.deposit.port.out.PgGatewayPort;
import com.deposit.application.deposit.port.out.SaveDepositPort;
import com.deposit.common.exception.BusinessException;
import com.deposit.common.exception.ErrorCode;
import com.deposit.domain.deposit.Deposit;
import com.deposit.domain.deposit.vo.DepositStatus;
import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.deposit.vo.PaymentMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PayDepositService")
class PayDepositServiceTest {

    private static final UUID TRIP_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

    @Mock LoadDepositPort loadDepositPort;
    @Mock SaveDepositPort saveDepositPort;
    @Mock PgGatewayPort pgGatewayPort;

    @InjectMocks
    PayDepositService sut;

    @Test
    @DisplayName("카드 결제 성공 → PAID 상태, 30일 환불 안내 포함")
    void pay_card_success() {
        Deposit pending = Deposit.create(TRIP_ID, 100L, Money.wons(50000));
        given(loadDepositPort.findById(1L)).willReturn(Optional.of(pending));
        given(pgGatewayPort.getFeeRate(PaymentMethod.CARD)).willReturn(new BigDecimal("0.035"));
        given(pgGatewayPort.pay(any())).willReturn(
                new PgGatewayPort.PayResult(true, "PG-TXN-001", null));
        given(saveDepositPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        DepositResult result = sut.pay(PayDepositCommand.builder()
                .depositId(1L).userId(100L).paymentMethod(PaymentMethod.CARD).build());

        assertThat(result.getStatus()).isEqualTo(DepositStatus.PAID);
        assertThat(result.getRefundNotice()).contains("30일").contains("3.5%");
        then(pgGatewayPort).should().pay(any());
        then(saveDepositPort).should().save(any());
    }

    @Test
    @DisplayName("계좌이체 결제 성공 → 수수료율 0.5% 안내")
    void pay_bankTransfer_success() {
        Deposit pending = Deposit.create(TRIP_ID, 100L, Money.wons(50000));
        given(loadDepositPort.findById(1L)).willReturn(Optional.of(pending));
        given(pgGatewayPort.getFeeRate(PaymentMethod.BANK_TRANSFER)).willReturn(new BigDecimal("0.005"));
        given(pgGatewayPort.pay(any())).willReturn(
                new PgGatewayPort.PayResult(true, "PG-TXN-002", null));
        given(saveDepositPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        DepositResult result = sut.pay(PayDepositCommand.builder()
                .depositId(1L).userId(100L).paymentMethod(PaymentMethod.BANK_TRANSFER).build());

        assertThat(result.getStatus()).isEqualTo(DepositStatus.PAID);
        assertThat(result.getRefundNotice()).contains("0.5%");
    }

    @Test
    @DisplayName("PG 결제 실패 → PG_PAYMENT_FAILED 예외")
    void pay_pgFailed_throws() {
        Deposit pending = Deposit.create(TRIP_ID, 100L, Money.wons(50000));
        given(loadDepositPort.findById(1L)).willReturn(Optional.of(pending));
        given(pgGatewayPort.getFeeRate(any())).willReturn(new BigDecimal("0.035"));
        given(pgGatewayPort.pay(any())).willReturn(
                new PgGatewayPort.PayResult(false, null, "카드 한도 초과"));

        assertThatThrownBy(() -> sut.pay(PayDepositCommand.builder()
                .depositId(1L).userId(100L).paymentMethod(PaymentMethod.CARD).build()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PG_PAYMENT_FAILED);

        then(saveDepositPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("존재하지 않는 보증금 결제 시 예외")
    void pay_depositNotFound_throws() {
        given(loadDepositPort.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.pay(PayDepositCommand.builder()
                .depositId(999L).userId(100L).paymentMethod(PaymentMethod.CARD).build()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DEPOSIT_NOT_FOUND);
    }
}
