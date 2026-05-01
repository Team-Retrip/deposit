package com.deposit.application.deposit.service;

import com.deposit.application.deposit.dto.result.TripDepositStatusResult;
import com.deposit.application.deposit.port.out.LoadDepositPort;
import com.deposit.common.exception.BusinessException;
import com.deposit.common.exception.ErrorCode;
import com.deposit.domain.deposit.Deposit;
import com.deposit.domain.deposit.vo.DepositStatus;
import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.deposit.vo.PaymentMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DepositQueryService")
class DepositQueryServiceTest {

    private static final UUID TRIP_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

    @Mock LoadDepositPort loadDepositPort;

    @InjectMocks
    DepositQueryService sut;

    @Test
    @DisplayName("전원 납부 → allPaid=true, 여행 시작 가능 메시지")
    void getStatus_allPaid() {
        given(loadDepositPort.findAllByTripId(TRIP_ID)).willReturn(List.of(
                paidDeposit(101L), paidDeposit(102L), paidDeposit(103L)
        ));

        TripDepositStatusResult result = sut.getStatus(TRIP_ID);

        assertThat(result.isAllPaid()).isTrue();
        assertThat(result.getPaidCount()).isEqualTo(3);
        assertThat(result.getUnpaidCount()).isEqualTo(0);
        assertThat(result.getTripStartMessage()).contains("여행을 시작할 수 있습니다");
    }

    @Test
    @DisplayName("미납자 있음 → allPaid=false, 여행 시작 불가 메시지")
    void getStatus_hasUnpaid() {
        given(loadDepositPort.findAllByTripId(TRIP_ID)).willReturn(List.of(
                paidDeposit(101L), pendingDeposit(102L), pendingDeposit(103L)
        ));

        TripDepositStatusResult result = sut.getStatus(TRIP_ID);

        assertThat(result.isAllPaid()).isFalse();
        assertThat(result.getPaidCount()).isEqualTo(1);
        assertThat(result.getUnpaidCount()).isEqualTo(2);
        assertThat(result.getTripStartMessage()).contains("미납 인원 2명").contains("시작할 수 없습니다");
    }

    @Test
    @DisplayName("참가자 없음 → allPaid=false (여행 시작 불가)")
    void getStatus_noParticipants() {
        given(loadDepositPort.findAllByTripId(TRIP_ID)).willReturn(List.of());

        assertThat(sut.areAllDepositsPaid(TRIP_ID)).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 보증금 조회 시 예외")
    void getDeposit_notFound_throws() {
        given(loadDepositPort.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.getDeposit(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DEPOSIT_NOT_FOUND);
    }

    private Deposit paidDeposit(Long userId) {
        return Deposit.builder()
                .id(userId).tripId(TRIP_ID).userId(userId)
                .amount(Money.wons(50000)).status(DepositStatus.PAID)
                .pgTransactionId("PG-TXN-" + userId)
                .pgFeeRate(new BigDecimal("0.035"))
                .paymentMethod(PaymentMethod.CARD)
                .paidAt(LocalDateTime.now())
                .refundDeadline(LocalDateTime.now().plusDays(30))
                .createdAt(LocalDateTime.now())
                .build();
    }

    private Deposit pendingDeposit(Long userId) {
        return Deposit.create(TRIP_ID, userId, Money.wons(50000));
    }
}
