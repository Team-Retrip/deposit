package com.deposit.application.deposit.service;

import com.deposit.application.deposit.dto.command.RefundDepositCommand;
import com.deposit.application.deposit.dto.result.AutoRefundResult;
import com.deposit.application.deposit.port.in.RefundDepositUseCase;
import com.deposit.application.deposit.port.out.LoadDepositPolicyPort;
import com.deposit.application.deposit.port.out.LoadDepositPort;
import com.deposit.application.deposit.port.out.SaveDepositPolicyPort;
import com.deposit.domain.deposit.Deposit;
import com.deposit.domain.deposit.DepositPolicy;
import com.deposit.domain.deposit.vo.DepositStatus;
import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.deposit.vo.PaymentMethod;
import com.deposit.domain.deposit.vo.PgProvider;
import com.deposit.domain.deposit.vo.PolicyStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AutoRefundUnconfirmedTripsService — 만료 자동 환불")
class AutoRefundUnconfirmedTripsServiceTest {

    private static final UUID TRIP_A = UUID.fromString("aaaa0000-0000-0000-0000-000000000001");
    private static final UUID TRIP_B = UUID.fromString("aaaa0000-0000-0000-0000-000000000002");

    @Mock LoadDepositPolicyPort loadDepositPolicyPort;
    @Mock SaveDepositPolicyPort saveDepositPolicyPort;
    @Mock LoadDepositPort loadDepositPort;
    @Mock RefundDepositUseCase refundDepositUseCase;

    @InjectMocks
    AutoRefundUnconfirmedTripsService sut;

    private DepositPolicy expiredPolicy(UUID tripId) {
        return DepositPolicy.builder()
                .id(1L).tripId(tripId).organizerId(10L)
                .depositAmount(Money.wons(50000)).status(PolicyStatus.ACTIVE)
                .pgProvider(PgProvider.TOSS_PAYMENTS)
                .tripStartAt(LocalDateTime.now().minusHours(1))
                .createdAt(LocalDateTime.now().minusDays(10))
                .build();
    }

    private Deposit paidDeposit(Long id, UUID tripId, Long userId) {
        return Deposit.builder()
                .id(id).tripId(tripId).userId(userId)
                .amount(Money.wons(50000)).status(DepositStatus.PAID)
                .pgTransactionId("PG-TXN-" + id)
                .pgFeeRate(new BigDecimal("0.035"))
                .paymentMethod(PaymentMethod.CARD)
                .paidAt(LocalDateTime.now().minusDays(5))
                .refundDeadline(LocalDateTime.now().plusDays(25))
                .createdAt(LocalDateTime.now().minusDays(10))
                .build();
    }

    private Deposit pendingDeposit(Long id, UUID tripId, Long userId) {
        return Deposit.builder()
                .id(id).tripId(tripId).userId(userId)
                .amount(Money.wons(50000)).status(DepositStatus.PENDING)
                .createdAt(LocalDateTime.now().minusDays(10))
                .build();
    }

    @Test
    @DisplayName("만료된 여행 없음 → processedTrips=0, refundedDeposits=0")
    void autoRefund_noExpiredTrips_returnsEmpty() {
        given(loadDepositPolicyPort.findAllActiveExpiredBefore(any())).willReturn(List.of());

        AutoRefundResult result = sut.autoRefundExpiredTrips();

        assertThat(result.getProcessedTrips()).isZero();
        assertThat(result.getRefundedDeposits()).isZero();
        then(refundDepositUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("만료된 여행 1개, 납부자 2명 → 2건 환불, 정책 CANCELLED")
    void autoRefund_oneExpiredTrip_twoDeposits_refundsAll() {
        DepositPolicy policy = expiredPolicy(TRIP_A);
        given(loadDepositPolicyPort.findAllActiveExpiredBefore(any())).willReturn(List.of(policy));
        given(loadDepositPort.findAllByTripId(TRIP_A)).willReturn(List.of(
                paidDeposit(1L, TRIP_A, 101L),
                paidDeposit(2L, TRIP_A, 102L)
        ));
        given(saveDepositPolicyPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        AutoRefundResult result = sut.autoRefundExpiredTrips();

        assertThat(result.getProcessedTrips()).isEqualTo(1);
        assertThat(result.getRefundedDeposits()).isEqualTo(2);
        assertThat(result.getCancelledTripIds()).containsExactly(TRIP_A);
        then(refundDepositUseCase).should(times(2)).refund(any(RefundDepositCommand.class));
        then(saveDepositPolicyPort).should().save(argThat(p -> p.isCancelled()));
    }

    @Test
    @DisplayName("미납 사용자는 환불 대상에서 제외")
    void autoRefund_pendingDepositsSkipped() {
        DepositPolicy policy = expiredPolicy(TRIP_A);
        given(loadDepositPolicyPort.findAllActiveExpiredBefore(any())).willReturn(List.of(policy));
        given(loadDepositPort.findAllByTripId(TRIP_A)).willReturn(List.of(
                paidDeposit(1L, TRIP_A, 101L),
                pendingDeposit(2L, TRIP_A, 102L)  // 미납 → 환불 대상 아님
        ));
        given(saveDepositPolicyPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        AutoRefundResult result = sut.autoRefundExpiredTrips();

        assertThat(result.getRefundedDeposits()).isEqualTo(1);
        then(refundDepositUseCase).should(times(1)).refund(any());
    }

    @Test
    @DisplayName("만료된 여행 2개 → 각각 환불 처리, 총 환불 건수 합산")
    void autoRefund_twoExpiredTrips_processedIndependently() {
        given(loadDepositPolicyPort.findAllActiveExpiredBefore(any()))
                .willReturn(List.of(expiredPolicy(TRIP_A), expiredPolicy(TRIP_B)));
        given(loadDepositPort.findAllByTripId(TRIP_A)).willReturn(List.of(
                paidDeposit(1L, TRIP_A, 101L)));
        given(loadDepositPort.findAllByTripId(TRIP_B)).willReturn(List.of(
                paidDeposit(2L, TRIP_B, 201L),
                paidDeposit(3L, TRIP_B, 202L)));
        given(saveDepositPolicyPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        AutoRefundResult result = sut.autoRefundExpiredTrips();

        assertThat(result.getProcessedTrips()).isEqualTo(2);
        assertThat(result.getRefundedDeposits()).isEqualTo(3);
        assertThat(result.getCancelledTripIds()).containsExactlyInAnyOrder(TRIP_A, TRIP_B);
    }
}
