package com.deposit.application.settlement.service;

import com.deposit.application.settlement.dto.command.SplitSettlementCommand;
import com.deposit.application.settlement.dto.result.SplitSettlementResult;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SplitSettlementService — 1/N 균등 분할")
class SplitSettlementServiceTest {

    private static final UUID TRIP_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final Long PAYER_ID = 1L;
    private static final Long DEBTOR_A = 10L;
    private static final Long DEBTOR_B = 20L;
    private static final Long DEBTOR_C = 30L;

    @Mock SaveSettlementPort saveSettlementPort;
    @Mock LoadSettlementBalancePort loadSettlementBalancePort;
    @Mock SaveSettlementBalancePort saveSettlementBalancePort;
    @Mock SettlementNotificationPort notificationPort;

    @InjectMocks
    SplitSettlementService sut;

    @Test
    @DisplayName("즉시 정산 3명 균등 분할 → 각자 알림 발송, status=NOTIFIED")
    void split_immediate_even_notifiesAll() {
        given(saveSettlementPort.save(any())).willAnswer(inv -> savedWithId(inv.getArgument(0)));

        SplitSettlementResult result = sut.split(command(60000, SettlementType.IMMEDIATE,
                List.of(DEBTOR_A, DEBTOR_B, DEBTOR_C)));

        // 각자에게 알림 발송
        then(notificationPort).should().notifyEqualSplitSettlement(
                eq(DEBTOR_A), eq(PAYER_ID), eq(Money.wons(20000)), any());
        then(notificationPort).should().notifyEqualSplitSettlement(
                eq(DEBTOR_B), eq(PAYER_ID), eq(Money.wons(20000)), any());
        then(notificationPort).should().notifyEqualSplitSettlement(
                eq(DEBTOR_C), eq(PAYER_ID), eq(Money.wons(20000)), any());

        assertThat(result.getSettlements()).hasSize(3);
        assertThat(result.getSettlements()).allMatch(s -> s.getStatus() == SettlementStatus.NOTIFIED);
        assertThat(result.getBaseAmount().intValue()).isEqualTo(20000);
        assertThat(result.getRemainderAmount()).isNull();
        assertThat(result.getTotalDebtors()).isEqualTo(3);
        assertThat(result.getNotificationMessage()).contains("3명");
    }

    @Test
    @DisplayName("나머지 발생 시 첫 번째 채무자에게 추가")
    void split_immediate_remainder_addedToFirst() {
        // 10001 / 3 = 3333 base, remainder = 2 → A=3335, B=3333, C=3333
        given(saveSettlementPort.save(any())).willAnswer(inv -> savedWithId(inv.getArgument(0)));

        SplitSettlementResult result = sut.split(command(10001, SettlementType.IMMEDIATE,
                List.of(DEBTOR_A, DEBTOR_B, DEBTOR_C)));

        assertThat(result.getBaseAmount().intValue()).isEqualTo(3333);
        assertThat(result.getRemainderAmount().intValue()).isEqualTo(2);

        // 첫 번째 채무자(A)는 base + remainder = 3335
        then(notificationPort).should().notifyEqualSplitSettlement(
                eq(DEBTOR_A), eq(PAYER_ID), eq(Money.wons(3335)), any());
        // B, C는 base = 3333
        then(notificationPort).should().notifyEqualSplitSettlement(
                eq(DEBTOR_B), eq(PAYER_ID), eq(Money.wons(3333)), any());
        then(notificationPort).should().notifyEqualSplitSettlement(
                eq(DEBTOR_C), eq(PAYER_ID), eq(Money.wons(3333)), any());
    }

    @Test
    @DisplayName("나중에 정산 분할 → 알림 없이 각 채무자 잔액 누적")
    void split_deferred_accumulatesBalancePerDebtor() {
        given(loadSettlementBalancePort.findByTripIdAndPayerIdAndDebtorId(any(), any(), any()))
                .willReturn(Optional.empty());
        given(saveSettlementBalancePort.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(saveSettlementPort.save(any())).willAnswer(inv -> savedWithId(inv.getArgument(0)));

        SplitSettlementResult result = sut.split(command(60000, SettlementType.DEFERRED,
                List.of(DEBTOR_A, DEBTOR_B)));

        then(notificationPort).shouldHaveNoInteractions();
        // 2명 각자 30000 잔액 저장
        then(saveSettlementBalancePort).should(times(2)).save(argThat(b ->
                b.getTotalAmount().equals(Money.wons(30000))
        ));
        assertThat(result.getSettlements()).allMatch(s -> s.getStatus() == SettlementStatus.ACCUMULATED);
        assertThat(result.getNotificationMessage()).isNull();
    }

    @Test
    @DisplayName("나중에 정산 기존 잔액에 누적")
    void split_deferred_accumulatesToExistingBalance() {
        SettlementBalance existing = SettlementBalance.create(TRIP_ID, PAYER_ID, DEBTOR_A);
        existing.accumulate(Money.wons(10000));
        given(loadSettlementBalancePort.findByTripIdAndPayerIdAndDebtorId(TRIP_ID, PAYER_ID, DEBTOR_A))
                .willReturn(Optional.of(existing));
        given(saveSettlementBalancePort.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(saveSettlementPort.save(any())).willAnswer(inv -> savedWithId(inv.getArgument(0)));

        sut.split(command(20000, SettlementType.DEFERRED, List.of(DEBTOR_A)));

        then(saveSettlementBalancePort).should().save(argThat(b ->
                b.getTotalAmount().equals(Money.wons(30000)) // 10000 + 20000
        ));
    }

    // ──────────────────────────────────────────────
    // 헬퍼
    // ──────────────────────────────────────────────

    private SplitSettlementCommand command(long total, SettlementType type, List<Long> debtorIds) {
        return SplitSettlementCommand.builder()
                .tripId(TRIP_ID).payerId(PAYER_ID)
                .totalAmount(Money.wons(total)).description("저녁")
                .type(type).debtorIds(debtorIds)
                .build();
    }

    private Settlement savedWithId(Settlement s) {
        return Settlement.builder()
                .id(1L).tripId(s.getTripId()).payerId(s.getPayerId()).debtorId(s.getDebtorId())
                .amount(s.getAmount()).description(s.getDescription())
                .type(s.getType()).status(s.getStatus()).createdAt(LocalDateTime.now())
                .build();
    }
}
