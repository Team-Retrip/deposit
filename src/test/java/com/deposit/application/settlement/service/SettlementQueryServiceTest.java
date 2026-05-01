package com.deposit.application.settlement.service;

import com.deposit.application.settlement.dto.result.SettlementBalanceResult;
import com.deposit.application.settlement.dto.result.SettlementResult;
import com.deposit.application.settlement.port.out.LoadSettlementBalancePort;
import com.deposit.application.settlement.port.out.LoadSettlementPort;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SettlementQueryService")
class SettlementQueryServiceTest {

    private static final UUID TRIP_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final Long PAYER_ID = 10L;
    private static final Long DEBTOR_A = 20L;
    private static final Long DEBTOR_B = 30L;

    @Mock LoadSettlementPort loadSettlementPort;
    @Mock LoadSettlementBalancePort loadSettlementBalancePort;

    @InjectMocks
    SettlementQueryService sut;

    @Test
    @DisplayName("여행의 전체 정산 내역 조회")
    void getByTrip_returnsAll() {
        given(loadSettlementPort.findAllByTripId(TRIP_ID)).willReturn(List.of(
                settlement(1L, SettlementType.IMMEDIATE, DEBTOR_A),
                settlement(2L, SettlementType.DEFERRED, DEBTOR_B)
        ));

        List<SettlementResult> results = sut.getByTrip(TRIP_ID);

        assertThat(results).hasSize(2);
        assertThat(results).extracting(SettlementResult::getType)
                .containsExactly(SettlementType.IMMEDIATE, SettlementType.DEFERRED);
    }

    @Test
    @DisplayName("여행의 전체 누적 잔액 조회")
    void getBalancesByTrip_returnsAll() {
        given(loadSettlementBalancePort.findAllByTripId(TRIP_ID)).willReturn(List.of(
                balance(1L, DEBTOR_A, Money.wons(15000)),
                balance(2L, DEBTOR_B, Money.wons(40000))
        ));

        List<SettlementBalanceResult> results = sut.getBalancesByTrip(TRIP_ID);

        assertThat(results).hasSize(2);
        assertThat(results).extracting(r -> r.getTotalAmount().intValue())
                .containsExactlyInAnyOrder(15000, 40000);
    }

    @Test
    @DisplayName("특정 채무자의 누적 잔액만 조회")
    void getBalancesByTripAndDebtor_filtered() {
        given(loadSettlementBalancePort.findAllByTripIdAndDebtorId(TRIP_ID, DEBTOR_A)).willReturn(List.of(
                balance(1L, DEBTOR_A, Money.wons(15000))
        ));

        List<SettlementBalanceResult> results = sut.getBalancesByTripAndDebtor(TRIP_ID, DEBTOR_A);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getDebtorId()).isEqualTo(DEBTOR_A);
    }

    @Test
    @DisplayName("정산 내역 없는 여행 → 빈 목록")
    void getByTrip_empty() {
        given(loadSettlementPort.findAllByTripId(TRIP_ID)).willReturn(List.of());

        assertThat(sut.getByTrip(TRIP_ID)).isEmpty();
    }

    // ──────────────────────────────────────────────
    // 헬퍼
    // ──────────────────────────────────────────────

    private Settlement settlement(Long id, SettlementType type, Long debtorId) {
        SettlementStatus status = type == SettlementType.IMMEDIATE
                ? SettlementStatus.NOTIFIED : SettlementStatus.ACCUMULATED;
        return Settlement.builder()
                .id(id).tripId(TRIP_ID).payerId(PAYER_ID).debtorId(debtorId)
                .amount(Money.wons(10000)).description("테스트")
                .type(type).status(status).createdAt(LocalDateTime.now())
                .build();
    }

    private SettlementBalance balance(Long id, Long debtorId, Money total) {
        SettlementBalance b = SettlementBalance.create(TRIP_ID, PAYER_ID, debtorId);
        b.accumulate(total);
        return SettlementBalance.builder()
                .id(id).tripId(TRIP_ID).payerId(PAYER_ID).debtorId(debtorId)
                .totalAmount(total).lastUpdatedAt(LocalDateTime.now())
                .build();
    }
}
