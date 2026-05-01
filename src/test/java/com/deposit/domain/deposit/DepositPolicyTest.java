package com.deposit.domain.deposit;

import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.deposit.vo.PolicyStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("DepositPolicy 도메인")
class DepositPolicyTest {

    private static final UUID TRIP_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

    @Test
    @DisplayName("정책 생성 시 ACTIVE 상태")
    void create_isActive() {
        DepositPolicy policy = DepositPolicy.create(TRIP_ID, 10L, Money.wons(50000));

        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.ACTIVE);
        assertThat(policy.isActive()).isTrue();
        assertThat(policy.getTripId()).isEqualTo(TRIP_ID);
        assertThat(policy.getOrganizerId()).isEqualTo(10L);
        assertThat(policy.getDepositAmount()).isEqualTo(Money.wons(50000));
    }

    @Test
    @DisplayName("정책 발급 → 보증금 생성 (tripId, userId, amount 일치)")
    void issueDeposit_createsCorrectDeposit() {
        DepositPolicy policy = DepositPolicy.create(TRIP_ID, 10L, Money.wons(50000));
        Deposit deposit = policy.issueDeposit(200L);

        assertThat(deposit.getTripId()).isEqualTo(TRIP_ID);
        assertThat(deposit.getUserId()).isEqualTo(200L);
        assertThat(deposit.getAmount()).isEqualTo(Money.wons(50000));
    }

    @Test
    @DisplayName("정책 비활성화")
    void deactivate() {
        DepositPolicy policy = DepositPolicy.create(TRIP_ID, 10L, Money.wons(50000));
        policy.deactivate();

        assertThat(policy.isActive()).isFalse();
        assertThat(policy.getStatus()).isEqualTo(PolicyStatus.INACTIVE);
    }
}
