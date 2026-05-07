package com.deposit.application.settlement.service;

import com.deposit.application.settlement.dto.command.CancelSettlementCommand;
import com.deposit.application.settlement.dto.result.SettlementResult;
import com.deposit.application.settlement.port.out.LoadSettlementBalancePort;
import com.deposit.application.settlement.port.out.LoadSettlementPort;
import com.deposit.application.settlement.port.out.SaveSettlementBalancePort;
import com.deposit.application.settlement.port.out.SaveSettlementPort;
import com.deposit.application.settlement.port.out.SettlementNotificationPort;
import com.deposit.common.exception.BusinessException;
import com.deposit.common.exception.ErrorCode;
import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.settlement.Settlement;
import com.deposit.domain.settlement.SettlementBalance;
import com.deposit.domain.settlement.vo.SettlementStatus;
import com.deposit.domain.settlement.vo.SettlementType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CancelSettlementService")
class CancelSettlementServiceTest {

    private static final UUID TRIP_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final Long PAYER_ID = 10L;
    private static final Long DEBTOR_ID = 20L;
    private static final Long SETTLEMENT_ID = 1L;
    private static final Money AMOUNT = Money.wons(30000);

    @Mock LoadSettlementPort loadSettlementPort;
    @Mock SaveSettlementPort saveSettlementPort;
    @Mock LoadSettlementBalancePort loadSettlementBalancePort;
    @Mock SaveSettlementBalancePort saveSettlementBalancePort;
    @Mock SettlementNotificationPort notificationPort;

    @InjectMocks
    CancelSettlementService sut;

    @Test
    @DisplayName("즉시 정산 취소 → 취소 알림 발송, status=CANCELLED, 알림 메시지 포함")
    void cancel_immediate_notifiesCancellationAndReturnsMessage() {
        given(loadSettlementPort.findById(SETTLEMENT_ID))
                .willReturn(Optional.of(immediateSettlement(AMOUNT)));
        given(saveSettlementPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        SettlementResult result = sut.cancel(command());

        then(notificationPort).should()
                .notifySettlementCancelled(DEBTOR_ID, PAYER_ID, AMOUNT, "점심값");
        then(notificationPort).shouldHaveNoMoreInteractions();
        then(loadSettlementBalancePort).shouldHaveNoInteractions();
        then(saveSettlementBalancePort).shouldHaveNoInteractions();

        assertThat(result.getStatus()).isEqualTo(SettlementStatus.CANCELLED);
        assertThat(result.getNotificationMessage()).isNotNull().contains(String.valueOf(DEBTOR_ID));
    }

    @Test
    @DisplayName("나중에 정산 취소 → 누적 잔액 차감, status=CANCELLED, 알림 없음")
    void cancel_deferred_deductsBalanceAndNoNotification() {
        SettlementBalance balance = existingBalance(AMOUNT); // 잔액 30000

        given(loadSettlementPort.findById(SETTLEMENT_ID))
                .willReturn(Optional.of(deferredSettlement(AMOUNT)));
        given(loadSettlementBalancePort.findByTripIdAndPayerIdAndDebtorId(TRIP_ID, PAYER_ID, DEBTOR_ID))
                .willReturn(Optional.of(balance));
        given(saveSettlementBalancePort.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(saveSettlementPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        SettlementResult result = sut.cancel(command());

        then(notificationPort).shouldHaveNoInteractions();
        then(saveSettlementBalancePort).should().save(argThat(b ->
                b.getTotalAmount().equals(Money.ZERO) // 30000 - 30000 = 0
        ));

        assertThat(result.getStatus()).isEqualTo(SettlementStatus.CANCELLED);
        assertThat(result.getNotificationMessage()).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 정산 취소 → SETTLEMENT_NOT_FOUND 예외")
    void cancel_notFound_throws() {
        given(loadSettlementPort.findById(SETTLEMENT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.cancel(command()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SETTLEMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("이미 취소된 정산 재취소 → SETTLEMENT_ALREADY_CANCELLED 예외")
    void cancel_alreadyCancelled_throws() {
        Settlement cancelled = cancelledSettlement();
        given(loadSettlementPort.findById(SETTLEMENT_ID)).willReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> sut.cancel(command()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SETTLEMENT_ALREADY_CANCELLED);
    }

    @Test
    @DisplayName("나중에 정산인데 잔액 없음 → SETTLEMENT_BALANCE_NOT_FOUND 예외")
    void cancel_deferred_balanceNotFound_throws() {
        given(loadSettlementPort.findById(SETTLEMENT_ID))
                .willReturn(Optional.of(deferredSettlement(AMOUNT)));
        given(loadSettlementBalancePort.findByTripIdAndPayerIdAndDebtorId(any(), any(), any()))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.cancel(command()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SETTLEMENT_BALANCE_NOT_FOUND);
    }

    @Test
    @DisplayName("즉시 정산 취소 알림 실패 → 예외 전파, 저장 안 됨")
    void cancel_immediate_notificationFails_throws() {
        given(loadSettlementPort.findById(SETTLEMENT_ID))
                .willReturn(Optional.of(immediateSettlement(AMOUNT)));
        willThrow(new RuntimeException("알림 서비스 오류"))
                .given(notificationPort).notifySettlementCancelled(any(), any(), any(), any());

        assertThatThrownBy(() -> sut.cancel(command()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("알림 서비스 오류");

        then(saveSettlementPort).shouldHaveNoInteractions();
    }

    // ──────────────────────────────────────────────
    // 헬퍼
    // ──────────────────────────────────────────────

    private CancelSettlementCommand command() {
        return CancelSettlementCommand.builder().settlementId(SETTLEMENT_ID).build();
    }

    private Settlement immediateSettlement(Money amount) {
        Settlement s = Settlement.create(TRIP_ID, PAYER_ID, DEBTOR_ID, amount, "점심값", SettlementType.IMMEDIATE);
        s.notified();
        return withId(s, SETTLEMENT_ID);
    }

    private Settlement deferredSettlement(Money amount) {
        Settlement s = Settlement.create(TRIP_ID, PAYER_ID, DEBTOR_ID, amount, "숙박비", SettlementType.DEFERRED);
        s.accumulated();
        return withId(s, SETTLEMENT_ID);
    }

    private Settlement cancelledSettlement() {
        Settlement s = Settlement.create(TRIP_ID, PAYER_ID, DEBTOR_ID, AMOUNT, "점심값", SettlementType.IMMEDIATE);
        s.notified();
        s.cancel();
        return withId(s, SETTLEMENT_ID);
    }

    private Settlement withId(Settlement s, Long id) {
        return Settlement.builder()
                .id(id).tripId(s.getTripId()).payerId(s.getPayerId()).debtorId(s.getDebtorId())
                .amount(s.getAmount()).description(s.getDescription())
                .type(s.getType()).status(s.getStatus()).createdAt(LocalDateTime.now())
                .build();
    }

    private SettlementBalance existingBalance(Money amount) {
        SettlementBalance b = SettlementBalance.create(TRIP_ID, PAYER_ID, DEBTOR_ID);
        b.accumulate(amount);
        return b;
    }
}
