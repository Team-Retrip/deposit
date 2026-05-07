package com.deposit.application.settlement.service;

import com.deposit.application.settlement.dto.result.SettlementResult;
import com.deposit.application.settlement.port.out.LoadSettlementPort;
import com.deposit.application.settlement.port.out.SaveSettlementPort;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompleteSettlementService")
class CompleteSettlementServiceTest {

    private static final UUID TRIP_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final Long PAYER_ID = 10L;
    private static final Long DEBTOR_ID = 20L;
    private static final Long SETTLEMENT_ID = 1L;
    private static final Money AMOUNT = Money.wons(30000);

    @Mock LoadSettlementPort loadSettlementPort;
    @Mock SaveSettlementPort saveSettlementPort;

    @InjectMocks
    CompleteSettlementService sut;

    @Test
    @DisplayName("NOTIFIED 상태 즉시 정산 완료 → status=COMPLETED")
    void complete_notified_success() {
        given(loadSettlementPort.findById(SETTLEMENT_ID))
                .willReturn(Optional.of(notifiedSettlement()));
        given(saveSettlementPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        SettlementResult result = sut.complete(SETTLEMENT_ID);

        assertThat(result.getStatus()).isEqualTo(SettlementStatus.COMPLETED);
    }

    @Test
    @DisplayName("ACCUMULATED 상태 나중에 정산 완료 → status=COMPLETED")
    void complete_accumulated_success() {
        given(loadSettlementPort.findById(SETTLEMENT_ID))
                .willReturn(Optional.of(accumulatedSettlement()));
        given(saveSettlementPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        SettlementResult result = sut.complete(SETTLEMENT_ID);

        assertThat(result.getStatus()).isEqualTo(SettlementStatus.COMPLETED);
    }

    @Test
    @DisplayName("이미 완료된 정산 재완료 → SETTLEMENT_ALREADY_COMPLETED 예외")
    void complete_alreadyCompleted_throws() {
        Settlement completed = notifiedSettlement();
        completed.complete();
        given(loadSettlementPort.findById(SETTLEMENT_ID)).willReturn(Optional.of(withId(completed, SETTLEMENT_ID)));

        assertThatThrownBy(() -> sut.complete(SETTLEMENT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SETTLEMENT_ALREADY_COMPLETED);
    }

    @Test
    @DisplayName("취소된 정산 완료 처리 → SETTLEMENT_NOT_COMPLETABLE 예외")
    void complete_cancelled_throws() {
        Settlement cancelled = notifiedSettlement();
        cancelled.cancel();
        given(loadSettlementPort.findById(SETTLEMENT_ID)).willReturn(Optional.of(withId(cancelled, SETTLEMENT_ID)));

        assertThatThrownBy(() -> sut.complete(SETTLEMENT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SETTLEMENT_NOT_COMPLETABLE);
    }

    @Test
    @DisplayName("존재하지 않는 정산 완료 → SETTLEMENT_NOT_FOUND 예외")
    void complete_notFound_throws() {
        given(loadSettlementPort.findById(SETTLEMENT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.complete(SETTLEMENT_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.SETTLEMENT_NOT_FOUND);
    }

    // ──────────────────────────────────────────────
    // 헬퍼
    // ──────────────────────────────────────────────

    private Settlement notifiedSettlement() {
        Settlement s = Settlement.create(TRIP_ID, PAYER_ID, DEBTOR_ID, AMOUNT, "점심값", SettlementType.IMMEDIATE);
        s.notified();
        return withId(s, SETTLEMENT_ID);
    }

    private Settlement accumulatedSettlement() {
        Settlement s = Settlement.create(TRIP_ID, PAYER_ID, DEBTOR_ID, AMOUNT, "숙박비", SettlementType.DEFERRED);
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
}
