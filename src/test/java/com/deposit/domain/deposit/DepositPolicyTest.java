package com.deposit.domain.deposit;

import com.deposit.common.exception.BusinessException;
import com.deposit.common.exception.ErrorCode;
import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.deposit.vo.PgProvider;
import com.deposit.domain.deposit.vo.PolicyStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("DepositPolicy 도메인")
class DepositPolicyTest {

    private static final UUID TRIP_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

    private DepositPolicy policy() {
        return DepositPolicy.create(TRIP_ID, 10L, Money.wons(50000),
                PgProvider.TOSS_PAYMENTS, LocalDateTime.now().plusDays(7));
    }

    @Test
    @DisplayName("정책 생성 시 ACTIVE 상태, PG사 및 여행 시작 시간 저장")
    void create_isActive() {
        DepositPolicy p = policy();

        assertThat(p.getStatus()).isEqualTo(PolicyStatus.ACTIVE);
        assertThat(p.isActive()).isTrue();
        assertThat(p.getTripId()).isEqualTo(TRIP_ID);
        assertThat(p.getOrganizerId()).isEqualTo(10L);
        assertThat(p.getDepositAmount()).isEqualTo(Money.wons(50000));
        assertThat(p.getPgProvider()).isEqualTo(PgProvider.TOSS_PAYMENTS);
        assertThat(p.getTripStartAt()).isNotNull();
    }

    @Test
    @DisplayName("정책 발급 → 보증금 생성 (tripId, userId, amount 일치)")
    void issueDeposit_createsCorrectDeposit() {
        Deposit deposit = policy().issueDeposit(200L);

        assertThat(deposit.getTripId()).isEqualTo(TRIP_ID);
        assertThat(deposit.getUserId()).isEqualTo(200L);
        assertThat(deposit.getAmount()).isEqualTo(Money.wons(50000));
    }

    @Test
    @DisplayName("정책 비활성화")
    void deactivate() {
        DepositPolicy p = policy();
        p.deactivate();

        assertThat(p.isActive()).isFalse();
        assertThat(p.getStatus()).isEqualTo(PolicyStatus.INACTIVE);
    }

    @Test
    @DisplayName("여행 확정 → CONFIRMED 상태")
    void confirm_setsConfirmed() {
        DepositPolicy p = policy();
        p.confirm();

        assertThat(p.isConfirmed()).isTrue();
        assertThat(p.getStatus()).isEqualTo(PolicyStatus.CONFIRMED);
    }

    @Test
    @DisplayName("이미 확정된 여행 재확정 시 예외")
    void confirm_alreadyConfirmed_throws() {
        DepositPolicy p = policy();
        p.confirm();

        assertThatThrownBy(p::confirm)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TRIP_ALREADY_CONFIRMED);
    }

    @Test
    @DisplayName("여행 자동 취소 → CANCELLED 상태")
    void cancel_setsCancelled() {
        DepositPolicy p = policy();
        p.cancel();

        assertThat(p.isCancelled()).isTrue();
        assertThat(p.getStatus()).isEqualTo(PolicyStatus.CANCELLED);
    }

    @Test
    @DisplayName("여행 시작 시간이 지났고 ACTIVE 상태이면 만료 여행")
    void isTripStartExpired_pastAndActive_true() {
        DepositPolicy p = DepositPolicy.create(TRIP_ID, 10L, Money.wons(50000),
                PgProvider.TOSS_PAYMENTS, LocalDateTime.now().minusMinutes(1));

        assertThat(p.isTripStartExpiredWithoutConfirmation()).isTrue();
    }

    @Test
    @DisplayName("여행 시작 시간이 지났어도 CONFIRMED이면 만료 대상 아님")
    void isTripStartExpired_confirmedNotExpired() {
        DepositPolicy p = DepositPolicy.create(TRIP_ID, 10L, Money.wons(50000),
                PgProvider.TOSS_PAYMENTS, LocalDateTime.now().minusMinutes(1));
        p.confirm();

        assertThat(p.isTripStartExpiredWithoutConfirmation()).isFalse();
    }
}
