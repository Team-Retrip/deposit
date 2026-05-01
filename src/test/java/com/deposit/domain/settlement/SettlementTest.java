package com.deposit.domain.settlement;

import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.settlement.vo.SettlementStatus;
import com.deposit.domain.settlement.vo.SettlementType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Settlement 도메인")
class SettlementTest {

    private static final UUID TRIP_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final Long PAYER_ID = 10L;
    private static final Long DEBTOR_ID = 20L;
    private static final Money AMOUNT = Money.wons(30000);

    @Nested
    @DisplayName("생성(create)")
    class Create {

        @Test
        @DisplayName("즉시 정산 생성 → IMMEDIATE 타입, PENDING 상태")
        void create_immediate() {
            Settlement settlement = Settlement.create(TRIP_ID, PAYER_ID, DEBTOR_ID, AMOUNT, "점심값", SettlementType.IMMEDIATE);

            assertThat(settlement.getType()).isEqualTo(SettlementType.IMMEDIATE);
            assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.PENDING);
            assertThat(settlement.isImmediate()).isTrue();
            assertThat(settlement.getTripId()).isEqualTo(TRIP_ID);
            assertThat(settlement.getPayerId()).isEqualTo(PAYER_ID);
            assertThat(settlement.getDebtorId()).isEqualTo(DEBTOR_ID);
            assertThat(settlement.getAmount()).isEqualTo(AMOUNT);
            assertThat(settlement.getDescription()).isEqualTo("점심값");
            assertThat(settlement.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("나중에 정산 생성 → DEFERRED 타입, PENDING 상태")
        void create_deferred() {
            Settlement settlement = Settlement.create(TRIP_ID, PAYER_ID, DEBTOR_ID, AMOUNT, "숙박비", SettlementType.DEFERRED);

            assertThat(settlement.getType()).isEqualTo(SettlementType.DEFERRED);
            assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.PENDING);
            assertThat(settlement.isImmediate()).isFalse();
        }
    }

    @Nested
    @DisplayName("상태 전이")
    class StatusTransition {

        @Test
        @DisplayName("notified() → NOTIFIED 상태로 전이")
        void notified() {
            Settlement settlement = Settlement.create(TRIP_ID, PAYER_ID, DEBTOR_ID, AMOUNT, "점심값", SettlementType.IMMEDIATE);
            settlement.notified();

            assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.NOTIFIED);
        }

        @Test
        @DisplayName("accumulated() → ACCUMULATED 상태로 전이")
        void accumulated() {
            Settlement settlement = Settlement.create(TRIP_ID, PAYER_ID, DEBTOR_ID, AMOUNT, "숙박비", SettlementType.DEFERRED);
            settlement.accumulated();

            assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.ACCUMULATED);
        }
    }

    @Nested
    @DisplayName("금액 수정(updateAmount)")
    class UpdateAmount {

        @Test
        @DisplayName("즉시 정산 금액 수정 → 새 금액으로 교체, 상태 유지")
        void update_immediate_changesAmount() {
            Settlement settlement = Settlement.create(TRIP_ID, PAYER_ID, DEBTOR_ID, AMOUNT, "점심값", SettlementType.IMMEDIATE);
            settlement.notified();

            settlement.updateAmount(Money.wons(50000));

            assertThat(settlement.getAmount()).isEqualTo(Money.wons(50000));
            assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.NOTIFIED);
        }

        @Test
        @DisplayName("나중에 정산 금액 수정 → 새 금액으로 교체, 상태 유지")
        void update_deferred_changesAmount() {
            Settlement settlement = Settlement.create(TRIP_ID, PAYER_ID, DEBTOR_ID, AMOUNT, "숙박비", SettlementType.DEFERRED);
            settlement.accumulated();

            settlement.updateAmount(Money.wons(15000));

            assertThat(settlement.getAmount()).isEqualTo(Money.wons(15000));
            assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.ACCUMULATED);
        }
    }
}
