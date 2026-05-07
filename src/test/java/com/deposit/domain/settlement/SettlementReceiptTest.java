package com.deposit.domain.settlement;

import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.settlement.vo.SettlementReceiptStatus;
import com.deposit.domain.settlement.vo.SettlementType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("SettlementReceipt 도메인")
class SettlementReceiptTest {

    private static final UUID TRIP_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final Long PAYER_ID = 1L;
    private static final Long DEBTOR_A = 10L;
    private static final Long DEBTOR_B = 20L;
    private static final Long DEBTOR_C = 30L;

    @Nested
    @DisplayName("생성(create)")
    class Create {

        @Test
        @DisplayName("영수증 생성 → ACTIVE 상태, items 포함")
        void create_basic() {
            List<SettlementReceiptItem> items = List.of(
                    SettlementReceiptItem.of(DEBTOR_A, Money.wons(20000)),
                    SettlementReceiptItem.of(DEBTOR_B, Money.wons(20000)),
                    SettlementReceiptItem.of(DEBTOR_C, Money.wons(20000))
            );

            SettlementReceipt receipt = SettlementReceipt.create(
                    TRIP_ID, PAYER_ID, Money.wons(60000), "저녁 식사", SettlementType.IMMEDIATE, items);

            assertThat(receipt.getStatus()).isEqualTo(SettlementReceiptStatus.ACTIVE);
            assertThat(receipt.getTotalAmount()).isEqualTo(Money.wons(60000));
            assertThat(receipt.getItems()).hasSize(3);
            assertThat(receipt.isCancelled()).isFalse();
            assertThat(receipt.isItemsSumMatchesTotal()).isTrue();
        }

        @Test
        @DisplayName("합계 != 총액인 경우 isItemsSumMatchesTotal = false")
        void create_mismatch_detectable() {
            List<SettlementReceiptItem> items = List.of(
                    SettlementReceiptItem.of(DEBTOR_A, Money.wons(20000)),
                    SettlementReceiptItem.of(DEBTOR_B, Money.wons(15000))
            );

            SettlementReceipt receipt = SettlementReceipt.create(
                    TRIP_ID, PAYER_ID, Money.wons(60000), "저녁 식사", SettlementType.IMMEDIATE, items);

            assertThat(receipt.isItemsSumMatchesTotal()).isFalse();
        }
    }

    @Nested
    @DisplayName("총액 수정(updateTotal) — 비례 배분")
    class UpdateTotal {

        @Test
        @DisplayName("총액 증가 → 각 인원 금액 비례 증가, 합계 == 새 총액")
        void updateTotal_increase_proportional() {
            // 균등 분할: A=20k, B=20k, C=20k → 총 60k
            SettlementReceipt receipt = receipt60k();

            receipt.updateTotal(Money.wons(90000));

            assertThat(receipt.getTotalAmount()).isEqualTo(Money.wons(90000));
            assertThat(receipt.isItemsSumMatchesTotal()).isTrue();
            // 비례 배분이므로 나머지는 마지막 인원에게 — 합계만 검증
            assertThat(receipt.getTotalItemsSum()).isEqualTo(Money.wons(90000));
        }

        @Test
        @DisplayName("총액 감소 → 각 인원 금액 비례 감소, 합계 == 새 총액")
        void updateTotal_decrease_proportional() {
            SettlementReceipt receipt = receipt60k();

            receipt.updateTotal(Money.wons(30000));

            assertThat(receipt.getTotalAmount()).isEqualTo(Money.wons(30000));
            assertThat(receipt.isItemsSumMatchesTotal()).isTrue();
        }

        @Test
        @DisplayName("불균등 비율 → 나머지는 마지막 인원에게, 합계 == 새 총액")
        void updateTotal_unevenRatio_remainderToLast() {
            // A=10k(25%), B=30k(75%) → 총 40k → 새 총액 90k
            List<SettlementReceiptItem> items = List.of(
                    SettlementReceiptItem.of(DEBTOR_A, Money.wons(10000)),
                    SettlementReceiptItem.of(DEBTOR_B, Money.wons(30000))
            );
            SettlementReceipt receipt = SettlementReceipt.create(
                    TRIP_ID, PAYER_ID, Money.wons(40000), "숙박비", SettlementType.DEFERRED, items);

            receipt.updateTotal(Money.wons(90000));

            assertThat(receipt.getTotalAmount()).isEqualTo(Money.wons(90000));
            assertThat(receipt.isItemsSumMatchesTotal()).isTrue();
        }
    }

    @Nested
    @DisplayName("개별 금액 수정(updateItems)")
    class UpdateItems {

        @Test
        @DisplayName("지정한 인원 금액 수정 → 해당 항목만 변경")
        void updateItems_partial() {
            SettlementReceipt receipt = receipt60k();

            receipt.updateItems(Map.of(
                    DEBTOR_A, Money.wons(30000),
                    DEBTOR_B, Money.wons(10000)
            ));

            assertThat(itemAmount(receipt, DEBTOR_A)).isEqualTo(Money.wons(30000));
            assertThat(itemAmount(receipt, DEBTOR_B)).isEqualTo(Money.wons(10000));
            assertThat(itemAmount(receipt, DEBTOR_C)).isEqualTo(Money.wons(20000)); // 변경 없음
        }

        @Test
        @DisplayName("수정 후 합계 == 총액이면 isItemsSumMatchesTotal = true")
        void updateItems_sumMatches() {
            SettlementReceipt receipt = receipt60k();

            // A: 20k→30k, B: 20k→10k (C 유지 20k → 합계 60k)
            receipt.updateItems(Map.of(
                    DEBTOR_A, Money.wons(30000),
                    DEBTOR_B, Money.wons(10000)
            ));

            assertThat(receipt.isItemsSumMatchesTotal()).isTrue();
        }

        @Test
        @DisplayName("수정 후 합계 != 총액이면 isItemsSumMatchesTotal = false")
        void updateItems_sumMismatch() {
            SettlementReceipt receipt = receipt60k();

            receipt.updateItems(Map.of(DEBTOR_A, Money.wons(50000)));

            assertThat(receipt.isItemsSumMatchesTotal()).isFalse();
        }
    }

    @Nested
    @DisplayName("취소(cancel)")
    class Cancel {

        @Test
        @DisplayName("취소 → CANCELLED 상태, isCancelled = true")
        void cancel() {
            SettlementReceipt receipt = receipt60k();
            receipt.cancel();

            assertThat(receipt.getStatus()).isEqualTo(SettlementReceiptStatus.CANCELLED);
            assertThat(receipt.isCancelled()).isTrue();
        }
    }

    @Nested
    @DisplayName("hasDebtor")
    class HasDebtor {

        @Test
        @DisplayName("존재하는 채무자 → true")
        void hasDebtor_exists() {
            assertThat(receipt60k().hasDebtor(DEBTOR_A)).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 채무자 → false")
        void hasDebtor_notExists() {
            assertThat(receipt60k().hasDebtor(99L)).isFalse();
        }
    }

    // ──────────────────────────────────────────────
    // 헬퍼
    // ──────────────────────────────────────────────

    private SettlementReceipt receipt60k() {
        List<SettlementReceiptItem> items = List.of(
                SettlementReceiptItem.of(DEBTOR_A, Money.wons(20000)),
                SettlementReceiptItem.of(DEBTOR_B, Money.wons(20000)),
                SettlementReceiptItem.of(DEBTOR_C, Money.wons(20000))
        );
        return SettlementReceipt.create(TRIP_ID, PAYER_ID, Money.wons(60000), "저녁 식사", SettlementType.IMMEDIATE, items);
    }

    private Money itemAmount(SettlementReceipt receipt, Long debtorId) {
        return receipt.getItems().stream()
                .filter(i -> i.getDebtorId().equals(debtorId))
                .findFirst().orElseThrow()
                .getAmount();
    }
}
