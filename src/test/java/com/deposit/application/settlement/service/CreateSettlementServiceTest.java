package com.deposit.application.settlement.service;

import com.deposit.application.settlement.dto.command.CreateSettlementCommand;
import com.deposit.application.settlement.dto.result.SettlementResult;
import com.deposit.application.settlement.port.out.LoadSettlementBalancePort;
import com.deposit.application.settlement.port.out.SaveSettlementBalancePort;
import com.deposit.application.settlement.port.out.SaveSettlementPort;
import com.deposit.application.settlement.port.out.SettlementNotificationPort;
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
@DisplayName("CreateSettlementService")
class CreateSettlementServiceTest {

    private static final UUID TRIP_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final Long PAYER_ID = 10L;
    private static final Long DEBTOR_ID = 20L;
    private static final Money AMOUNT = Money.wons(30000);

    @Mock SaveSettlementPort saveSettlementPort;
    @Mock LoadSettlementBalancePort loadSettlementBalancePort;
    @Mock SaveSettlementBalancePort saveSettlementBalancePort;
    @Mock SettlementNotificationPort notificationPort;

    @InjectMocks
    CreateSettlementService sut;

    @Test
    @DisplayName("즉시 정산 → 알림 발송, status=NOTIFIED, 알림 메시지 포함")
    void create_immediate_notifiesAndReturnsMessage() {
        given(saveSettlementPort.save(any())).willAnswer(inv -> savedWith(inv.getArgument(0), 1L));

        SettlementResult result = sut.create(immediateCommand());

        then(notificationPort).should().notifyImmediateSettlement(DEBTOR_ID, PAYER_ID, AMOUNT, "점심값");
        then(loadSettlementBalancePort).shouldHaveNoInteractions();
        then(saveSettlementBalancePort).shouldHaveNoInteractions();

        assertThat(result.getStatus()).isEqualTo(SettlementStatus.NOTIFIED);
        assertThat(result.getType()).isEqualTo(SettlementType.IMMEDIATE);
        assertThat(result.getNotificationMessage()).isNotNull().contains(String.valueOf(DEBTOR_ID));
    }

    @Test
    @DisplayName("나중에 정산 (잔액 없음) → 새 잔액 생성 후 누적, status=ACCUMULATED")
    void create_deferred_createsNewBalance() {
        given(loadSettlementBalancePort.findByTripIdAndPayerIdAndDebtorId(TRIP_ID, PAYER_ID, DEBTOR_ID))
                .willReturn(Optional.empty());
        given(saveSettlementBalancePort.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(saveSettlementPort.save(any())).willAnswer(inv -> savedWith(inv.getArgument(0), 2L));

        SettlementResult result = sut.create(deferredCommand());

        then(notificationPort).shouldHaveNoInteractions();
        then(saveSettlementBalancePort).should().save(argThat(b ->
                b.getTotalAmount().equals(AMOUNT)
        ));

        assertThat(result.getStatus()).isEqualTo(SettlementStatus.ACCUMULATED);
        assertThat(result.getType()).isEqualTo(SettlementType.DEFERRED);
        assertThat(result.getNotificationMessage()).isNull();
    }

    @Test
    @DisplayName("나중에 정산 (잔액 있음) → 기존 잔액에 누적")
    void create_deferred_accumulatesToExistingBalance() {
        SettlementBalance existing = SettlementBalance.create(TRIP_ID, PAYER_ID, DEBTOR_ID);
        existing.accumulate(Money.wons(20000));

        given(loadSettlementBalancePort.findByTripIdAndPayerIdAndDebtorId(TRIP_ID, PAYER_ID, DEBTOR_ID))
                .willReturn(Optional.of(existing));
        given(saveSettlementBalancePort.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(saveSettlementPort.save(any())).willAnswer(inv -> savedWith(inv.getArgument(0), 3L));

        sut.create(deferredCommand()); // AMOUNT = 30000

        then(saveSettlementBalancePort).should().save(argThat(b ->
                b.getTotalAmount().equals(Money.wons(50000)) // 20000 + 30000
        ));
    }

    @Test
    @DisplayName("즉시 정산 알림 실패 → 예외 전파, 정산 저장되지 않음")
    void create_immediate_notificationFails_throws() {
        willThrow(new RuntimeException("알림 서비스 오류"))
                .given(notificationPort).notifyImmediateSettlement(any(), any(), any(), any());

        assertThatThrownBy(() -> sut.create(immediateCommand()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("알림 서비스 오류");

        then(saveSettlementPort).shouldHaveNoInteractions();
    }

    // ──────────────────────────────────────────────
    // 헬퍼
    // ──────────────────────────────────────────────

    private CreateSettlementCommand immediateCommand() {
        return CreateSettlementCommand.builder()
                .tripId(TRIP_ID).payerId(PAYER_ID).debtorId(DEBTOR_ID)
                .amount(AMOUNT).description("점심값").type(SettlementType.IMMEDIATE)
                .build();
    }

    private CreateSettlementCommand deferredCommand() {
        return CreateSettlementCommand.builder()
                .tripId(TRIP_ID).payerId(PAYER_ID).debtorId(DEBTOR_ID)
                .amount(AMOUNT).description("숙박비").type(SettlementType.DEFERRED)
                .build();
    }

    private Settlement savedWith(Settlement s, Long id) {
        return Settlement.builder()
                .id(id).tripId(s.getTripId()).payerId(s.getPayerId()).debtorId(s.getDebtorId())
                .amount(s.getAmount()).description(s.getDescription())
                .type(s.getType()).status(s.getStatus()).createdAt(LocalDateTime.now())
                .build();
    }
}
