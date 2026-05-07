package com.deposit.application.deposit.service;

import com.deposit.application.deposit.dto.command.PayDepositCommand;
import com.deposit.application.deposit.dto.result.DepositResult;
import com.deposit.application.deposit.port.out.LoadDepositPolicyPort;
import com.deposit.application.deposit.port.out.LoadDepositPort;
import com.deposit.application.deposit.port.out.PgGatewayPort;
import com.deposit.application.deposit.port.out.SaveDepositPort;
import com.deposit.common.exception.BusinessException;
import com.deposit.common.exception.ErrorCode;
import com.deposit.domain.deposit.Deposit;
import com.deposit.domain.deposit.DepositPolicy;
import com.deposit.domain.deposit.vo.DepositStatus;
import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.deposit.vo.PaymentMethod;
import com.deposit.domain.deposit.vo.PgProvider;
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
@DisplayName("PayDepositService")
class PayDepositServiceTest {

    private static final UUID TRIP_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

    @Mock LoadDepositPort loadDepositPort;
    @Mock LoadDepositPolicyPort loadDepositPolicyPort;
    @Mock SaveDepositPort saveDepositPort;
    @Mock PgGatewayPort pgGatewayPort;

    @InjectMocks
    PayDepositService sut;

    private DepositPolicy tossPolicy() {
        return DepositPolicy.builder()
                .id(1L).tripId(TRIP_ID).organizerId(10L)
                .depositAmount(Money.wons(50000))
                .status(com.deposit.domain.deposit.vo.PolicyStatus.ACTIVE)
                .pgProvider(PgProvider.TOSS_PAYMENTS)
                .tripStartAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now()).build();
    }

    @Test
    @DisplayName("카드 결제 성공 → PAID 상태, 30일 환불 안내 포함 (Toss Payments)")
    void pay_card_success() {
        Deposit pending = Deposit.create(TRIP_ID, 100L, Money.wons(50000));
        given(loadDepositPort.findById(1L)).willReturn(Optional.of(pending));
        given(loadDepositPolicyPort.findByTripId(TRIP_ID)).willReturn(Optional.of(tossPolicy()));
        given(pgGatewayPort.getFeeRate(PaymentMethod.CARD)).willReturn(new BigDecimal("0.035"));
        given(pgGatewayPort.pay(any())).willReturn(
                new PgGatewayPort.PayResult(true, "PG-TXN-001", null));
        given(saveDepositPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        DepositResult result = sut.pay(PayDepositCommand.builder()
                .depositId(1L).userId(100L).paymentMethod(PaymentMethod.CARD).build());

        assertThat(result.getStatus()).isEqualTo(DepositStatus.PAID);
        assertThat(result.getRefundNotice()).contains("30일").contains("3.5%");
        then(pgGatewayPort).should().pay(argThat(cmd ->
                cmd.pgProvider() == PgProvider.TOSS_PAYMENTS));
    }

    @Test
    @DisplayName("계좌이체 결제 성공 → 수수료율 0.5% 안내")
    void pay_bankTransfer_success() {
        Deposit pending = Deposit.create(TRIP_ID, 100L, Money.wons(50000));
        given(loadDepositPort.findById(1L)).willReturn(Optional.of(pending));
        given(loadDepositPolicyPort.findByTripId(TRIP_ID)).willReturn(Optional.of(tossPolicy()));
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
    @DisplayName("KG_INICIS PG로 결제 시 KG_INICIS provider 전달")
    void pay_kgInicis_provider_passed() {
        Deposit pending = Deposit.create(TRIP_ID, 100L, Money.wons(50000));
        DepositPolicy kgPolicy = DepositPolicy.builder()
                .id(1L).tripId(TRIP_ID).organizerId(10L)
                .depositAmount(Money.wons(50000))
                .status(com.deposit.domain.deposit.vo.PolicyStatus.ACTIVE)
                .pgProvider(PgProvider.KG_INICIS)
                .tripStartAt(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now()).build();

        given(loadDepositPort.findById(1L)).willReturn(Optional.of(pending));
        given(loadDepositPolicyPort.findByTripId(TRIP_ID)).willReturn(Optional.of(kgPolicy));
        given(pgGatewayPort.getFeeRate(any())).willReturn(new BigDecimal("0.035"));
        given(pgGatewayPort.pay(any())).willReturn(
                new PgGatewayPort.PayResult(true, "PG-TXN-KG-001", null));
        given(saveDepositPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        sut.pay(PayDepositCommand.builder()
                .depositId(1L).userId(100L).paymentMethod(PaymentMethod.CARD).build());

        then(pgGatewayPort).should().pay(argThat(cmd ->
                cmd.pgProvider() == PgProvider.KG_INICIS));
    }

    @Test
    @DisplayName("PG 결제 실패 → PG_PAYMENT_FAILED 예외")
    void pay_pgFailed_throws() {
        Deposit pending = Deposit.create(TRIP_ID, 100L, Money.wons(50000));
        given(loadDepositPort.findById(1L)).willReturn(Optional.of(pending));
        given(loadDepositPolicyPort.findByTripId(TRIP_ID)).willReturn(Optional.of(tossPolicy()));
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
