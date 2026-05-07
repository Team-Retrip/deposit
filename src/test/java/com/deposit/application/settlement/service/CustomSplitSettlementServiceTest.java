package com.deposit.application.settlement.service;

import com.deposit.application.settlement.dto.command.CustomSplitSettlementCommand;
import com.deposit.application.settlement.dto.result.SplitSettlementResult;
import com.deposit.application.settlement.port.out.LoadSettlementBalancePort;
import com.deposit.application.settlement.port.out.SaveSettlementBalancePort;
import com.deposit.application.settlement.port.out.SaveSettlementPort;
import com.deposit.application.settlement.port.out.SettlementNotificationPort;
import com.deposit.common.exception.BusinessException;
import com.deposit.common.exception.ErrorCode;
import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.settlement.Settlement;
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
@DisplayName("CustomSplitSettlementService — 개별 금액 분할")
class CustomSplitSettlementServiceTest {

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
    CustomSplitSettlementService sut;

    @Test
    @DisplayName("개별 금액 합계 == 총액 → 즉시 정산 알림 발송 성공")
    void split_immediate_validSum_notifiesAll() {
        given(saveSettlementPort.save(any())).willAnswer(inv -> savedWithId(inv.getArgument(0)));

        SplitSettlementResult result = sut.split(command(60000, SettlementType.IMMEDIATE, List.of(
                debtorAmount(DEBTOR_A, 30000),
                debtorAmount(DEBTOR_B, 20000),
                debtorAmount(DEBTOR_C, 10000)
        )));

        then(notificationPort).should().notifyIndividualSplitSettlement(
                eq(DEBTOR_A), eq(PAYER_ID), eq(Money.wons(30000)), any());
        then(notificationPort).should().notifyIndividualSplitSettlement(
                eq(DEBTOR_B), eq(PAYER_ID), eq(Money.wons(20000)), any());
        then(notificationPort).should().notifyIndividualSplitSettlement(
                eq(DEBTOR_C), eq(PAYER_ID), eq(Money.wons(10000)), any());

        assertThat(result.getSettlements()).hasSize(3);
        assertThat(result.getSettlements()).allMatch(s -> s.getStatus() == SettlementStatus.NOTIFIED);
        assertThat(result.getTotalDebtors()).isEqualTo(3);
        assertThat(result.getNotificationMessage()).contains("3명");
    }

    @Test
    @DisplayName("개별 금액 합계 != 총액 → SETTLEMENT_CUSTOM_AMOUNTS_MISMATCH 예외")
    void split_sumMismatch_throws() {
        assertThatThrownBy(() -> sut.split(command(60000, SettlementType.IMMEDIATE, List.of(
                debtorAmount(DEBTOR_A, 30000),
                debtorAmount(DEBTOR_B, 20000) // 합계 50000 ≠ 60000
        ))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SETTLEMENT_CUSTOM_AMOUNTS_MISMATCH);

        then(saveSettlementPort).shouldHaveNoInteractions();
        then(notificationPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("개별 금액 나중에 정산 → 각 채무자에게 개별 금액 누적")
    void split_deferred_accumulatesPerAmount() {
        given(loadSettlementBalancePort.findByTripIdAndPayerIdAndDebtorId(any(), any(), any()))
                .willReturn(Optional.empty());
        given(saveSettlementBalancePort.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(saveSettlementPort.save(any())).willAnswer(inv -> savedWithId(inv.getArgument(0)));

        sut.split(command(60000, SettlementType.DEFERRED, List.of(
                debtorAmount(DEBTOR_A, 40000),
                debtorAmount(DEBTOR_B, 20000)
        )));

        then(notificationPort).shouldHaveNoInteractions();
        then(saveSettlementBalancePort).should().save(argThat(b ->
                b.getDebtorId().equals(DEBTOR_A) && b.getTotalAmount().equals(Money.wons(40000))
        ));
        then(saveSettlementBalancePort).should().save(argThat(b ->
                b.getDebtorId().equals(DEBTOR_B) && b.getTotalAmount().equals(Money.wons(20000))
        ));
    }

    // ──────────────────────────────────────────────
    // 헬퍼
    // ──────────────────────────────────────────────

    private CustomSplitSettlementCommand command(long total, SettlementType type,
                                                  List<CustomSplitSettlementCommand.DebtorAmount> debtorAmounts) {
        return CustomSplitSettlementCommand.builder()
                .tripId(TRIP_ID).payerId(PAYER_ID)
                .totalAmount(Money.wons(total)).description("저녁")
                .type(type).debtorAmounts(debtorAmounts)
                .build();
    }

    private CustomSplitSettlementCommand.DebtorAmount debtorAmount(long debtorId, long amount) {
        return CustomSplitSettlementCommand.DebtorAmount.builder()
                .debtorId(debtorId).amount(Money.wons(amount)).build();
    }

    private Settlement savedWithId(Settlement s) {
        return Settlement.builder()
                .id(1L).tripId(s.getTripId()).payerId(s.getPayerId()).debtorId(s.getDebtorId())
                .amount(s.getAmount()).description(s.getDescription())
                .type(s.getType()).status(s.getStatus()).createdAt(LocalDateTime.now())
                .build();
    }
}
