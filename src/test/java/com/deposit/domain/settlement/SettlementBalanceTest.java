package com.deposit.domain.settlement;

import com.deposit.domain.deposit.vo.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("SettlementBalance 도메인")
class SettlementBalanceTest {

    private static final UUID TRIP_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final Long PAYER_ID = 10L;
    private static final Long DEBTOR_ID = 20L;

    @Test
    @DisplayName("생성 시 totalAmount는 0원")
    void create_totalAmountIsZero() {
        SettlementBalance balance = SettlementBalance.create(TRIP_ID, PAYER_ID, DEBTOR_ID);

        assertThat(balance.getTripId()).isEqualTo(TRIP_ID);
        assertThat(balance.getPayerId()).isEqualTo(PAYER_ID);
        assertThat(balance.getDebtorId()).isEqualTo(DEBTOR_ID);
        assertThat(balance.getTotalAmount()).isEqualTo(Money.ZERO);
        assertThat(balance.getLastUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("accumulate 한 번 → totalAmount 증가")
    void accumulate_once() {
        SettlementBalance balance = SettlementBalance.create(TRIP_ID, PAYER_ID, DEBTOR_ID);
        balance.accumulate(Money.wons(15000));

        assertThat(balance.getTotalAmount()).isEqualTo(Money.wons(15000));
    }

    @Test
    @DisplayName("accumulate 여러 번 → totalAmount 누적")
    void accumulate_multiple() {
        SettlementBalance balance = SettlementBalance.create(TRIP_ID, PAYER_ID, DEBTOR_ID);

        balance.accumulate(Money.wons(10000));
        balance.accumulate(Money.wons(20000));
        balance.accumulate(Money.wons(5000));

        assertThat(balance.getTotalAmount()).isEqualTo(Money.wons(35000));
    }

    @Test
    @DisplayName("accumulate 시 lastUpdatedAt 갱신")
    void accumulate_updatesLastUpdatedAt() throws InterruptedException {
        SettlementBalance balance = SettlementBalance.create(TRIP_ID, PAYER_ID, DEBTOR_ID);
        var before = balance.getLastUpdatedAt();
        Thread.sleep(1);

        balance.accumulate(Money.wons(5000));

        assertThat(balance.getLastUpdatedAt()).isAfterOrEqualTo(before);
    }
}
