package com.deposit.application.settlement.service;

import com.deposit.application.settlement.dto.command.UpdateSettlementCommand;
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
@DisplayName("UpdateSettlementService")
class UpdateSettlementServiceTest {

    private static final UUID TRIP_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final Long PAYER_ID = 10L;
    private static final Long DEBTOR_ID = 20L;
    private static final Long SETTLEMENT_ID = 1L;
    private static final Money OLD_AMOUNT = Money.wons(30000);
    private static final Money NEW_AMOUNT = Money.wons(50000);

    @Mock LoadSettlementPort loadSettlementPort;
    @Mock SaveSettlementPort saveSettlementPort;
    @Mock LoadSettlementBalancePort loadSettlementBalancePort;
    @Mock SaveSettlementBalancePort saveSettlementBalancePort;
    @Mock SettlementNotificationPort notificationPort;

    @InjectMocks
    UpdateSettlementService sut;

    @Test
    @DisplayName("즉시 정산 금액 수정 → 수정된 금액으로 알림 재발송, 알림 메시지 반환")
    void update_immediate_resendNotification() {
        given(loadSettlementPort.findById(SETTLEMENT_ID))
                .willReturn(Optional.of(immediateSettlement(OLD_AMOUNT)));
        given(saveSettlementPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        SettlementResult result = sut.update(command(NEW_AMOUNT));

        then(notificationPort).should()
                .notifySettlementAmountUpdated(DEBTOR_ID, PAYER_ID, NEW_AMOUNT, "점심값");
        then(loadSettlementBalancePort).shouldHaveNoInteractions();
        then(saveSettlementBalancePort).shouldHaveNoInteractions();

        assertThat(result.getAmount()).isEqualByComparingTo("50000");
        assertThat(result.getStatus()).isEqualTo(SettlementStatus.NOTIFIED);
        assertThat(result.getNotificationMessage()).contains(String.valueOf(DEBTOR_ID));
    }

    @Test
    @DisplayName("나중에 정산 금액 증가 수정 → 잔액 증가 조정")
    void update_deferred_increaseAmount_adjustsBalance() {
        SettlementBalance balance = existingBalance(OLD_AMOUNT); // balance = 30000
        given(loadSettlementPort.findById(SETTLEMENT_ID))
                .willReturn(Optional.of(deferredSettlement(OLD_AMOUNT)));
        given(loadSettlementBalancePort.findByTripIdAndPayerIdAndDebtorId(TRIP_ID, PAYER_ID, DEBTOR_ID))
                .willReturn(Optional.of(balance));
        given(saveSettlementBalancePort.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(saveSettlementPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        SettlementResult result = sut.update(command(NEW_AMOUNT)); // 30000 → 50000

        then(notificationPort).shouldHaveNoInteractions();
        then(saveSettlementBalancePort).should().save(argThat(b ->
                b.getTotalAmount().equals(Money.wons(50000)) // 30000 - 30000 + 50000
        ));
        assertThat(result.getAmount()).isEqualByComparingTo("50000");
        assertThat(result.getNotificationMessage()).isNull();
    }

    @Test
    @DisplayName("나중에 정산 금액 감소 수정 → 잔액 감소 조정")
    void update_deferred_decreaseAmount_adjustsBalance() {
        Money decreasedAmount = Money.wons(10000);
        SettlementBalance balance = existingBalance(OLD_AMOUNT); // balance = 30000
        given(loadSettlementPort.findById(SETTLEMENT_ID))
                .willReturn(Optional.of(deferredSettlement(OLD_AMOUNT)));
        given(loadSettlementBalancePort.findByTripIdAndPayerIdAndDebtorId(TRIP_ID, PAYER_ID, DEBTOR_ID))
                .willReturn(Optional.of(balance));
        given(saveSettlementBalancePort.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(saveSettlementPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        sut.update(command(decreasedAmount)); // 30000 → 10000

        then(saveSettlementBalancePort).should().save(argThat(b ->
                b.getTotalAmount().equals(Money.wons(10000)) // 30000 - 30000 + 10000
        ));
    }

    @Test
    @DisplayName("존재하지 않는 정산 수정 → SETTLEMENT_NOT_FOUND 예외")
    void update_notFound_throws() {
        given(loadSettlementPort.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.update(UpdateSettlementCommand.builder()
                .settlementId(999L).newAmount(NEW_AMOUNT).build()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SETTLEMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("나중에 정산인데 잔액 없음 → SETTLEMENT_BALANCE_NOT_FOUND 예외")
    void update_deferred_balanceNotFound_throws() {
        given(loadSettlementPort.findById(SETTLEMENT_ID))
                .willReturn(Optional.of(deferredSettlement(OLD_AMOUNT)));
        given(loadSettlementBalancePort.findByTripIdAndPayerIdAndDebtorId(any(), any(), any()))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.update(command(NEW_AMOUNT)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SETTLEMENT_BALANCE_NOT_FOUND);
    }

    @Test
    @DisplayName("취소된 정산 수정 → SETTLEMENT_ALREADY_CANCELLED 예외")
    void update_cancelled_throws() {
        Settlement cancelled = Settlement.create(TRIP_ID, PAYER_ID, DEBTOR_ID, OLD_AMOUNT, "점심값", SettlementType.IMMEDIATE);
        cancelled.notified();
        cancelled.cancel();
        given(loadSettlementPort.findById(SETTLEMENT_ID)).willReturn(Optional.of(withId(cancelled, SETTLEMENT_ID)));

        assertThatThrownBy(() -> sut.update(command(NEW_AMOUNT)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SETTLEMENT_ALREADY_CANCELLED);

        then(saveSettlementPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("즉시 정산 알림 실패 → 예외 전파, 저장 안 됨")
    void update_immediate_notificationFails_throws() {
        given(loadSettlementPort.findById(SETTLEMENT_ID))
                .willReturn(Optional.of(immediateSettlement(OLD_AMOUNT)));
        willThrow(new RuntimeException("알림 서비스 오류"))
                .given(notificationPort).notifySettlementAmountUpdated(any(), any(), any(), any());

        assertThatThrownBy(() -> sut.update(command(NEW_AMOUNT)))
                .isInstanceOf(RuntimeException.class);

        then(saveSettlementPort).shouldHaveNoInteractions();
    }

    // ──────────────────────────────────────────────
    // 헬퍼
    // ──────────────────────────────────────────────

    private UpdateSettlementCommand command(Money newAmount) {
        return UpdateSettlementCommand.builder()
                .settlementId(SETTLEMENT_ID).newAmount(newAmount).build();
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
        return SettlementBalance.builder()
                .id(99L).tripId(TRIP_ID).payerId(PAYER_ID).debtorId(DEBTOR_ID)
                .totalAmount(amount).lastUpdatedAt(LocalDateTime.now())
                .build();
    }
}
