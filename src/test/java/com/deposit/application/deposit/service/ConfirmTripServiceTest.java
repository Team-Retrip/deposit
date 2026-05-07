package com.deposit.application.deposit.service;

import com.deposit.application.deposit.dto.command.ConfirmTripCommand;
import com.deposit.application.deposit.dto.result.ConfirmTripResult;
import com.deposit.application.deposit.dto.result.TripDepositStatusResult;
import com.deposit.application.deposit.port.in.GetTripDepositStatusUseCase;
import com.deposit.application.deposit.port.out.LoadDepositPolicyPort;
import com.deposit.application.deposit.port.out.SaveDepositPolicyPort;
import com.deposit.common.exception.BusinessException;
import com.deposit.common.exception.ErrorCode;
import com.deposit.domain.deposit.DepositPolicy;
import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.deposit.vo.PgProvider;
import com.deposit.domain.deposit.vo.PolicyStatus;
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
@DisplayName("ConfirmTripService — 여행 확정")
class ConfirmTripServiceTest {

    private static final UUID TRIP_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

    @Mock LoadDepositPolicyPort loadDepositPolicyPort;
    @Mock SaveDepositPolicyPort saveDepositPolicyPort;
    @Mock GetTripDepositStatusUseCase getTripDepositStatusUseCase;

    @InjectMocks
    ConfirmTripService sut;

    private DepositPolicy activePolicy() {
        return DepositPolicy.builder()
                .id(1L).tripId(TRIP_ID).organizerId(10L)
                .depositAmount(Money.wons(50000))
                .status(PolicyStatus.ACTIVE)
                .pgProvider(PgProvider.TOSS_PAYMENTS)
                .tripStartAt(LocalDateTime.now().plusDays(3))
                .createdAt(LocalDateTime.now())
                .build();
    }

    private TripDepositStatusResult allPaidStatus(int count) {
        return TripDepositStatusResult.builder()
                .tripId(TRIP_ID).totalCount(count).paidCount(count)
                .unpaidCount(0).allPaid(true).build();
    }

    @Test
    @DisplayName("전원 납부 완료 → 여행 확정 성공, CONFIRMED 상태")
    void confirm_allPaid_success() {
        given(loadDepositPolicyPort.findByTripId(TRIP_ID)).willReturn(Optional.of(activePolicy()));
        given(getTripDepositStatusUseCase.areAllDepositsPaid(TRIP_ID)).willReturn(true);
        given(saveDepositPolicyPort.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(getTripDepositStatusUseCase.getStatus(TRIP_ID)).willReturn(allPaidStatus(3));

        ConfirmTripResult result = sut.confirm(ConfirmTripCommand.builder()
                .tripId(TRIP_ID).organizerId(10L).build());

        assertThat(result.getStatus()).isEqualTo(PolicyStatus.CONFIRMED);
        assertThat(result.getConfirmedDeposits()).isEqualTo(3);
        then(saveDepositPolicyPort).should().save(argThat(p -> p.isConfirmed()));
    }

    @Test
    @DisplayName("미납 인원 있으면 확정 불가 → DEPOSIT_UNPAID_EXISTS 예외")
    void confirm_notAllPaid_throws() {
        given(loadDepositPolicyPort.findByTripId(TRIP_ID)).willReturn(Optional.of(activePolicy()));
        given(getTripDepositStatusUseCase.areAllDepositsPaid(TRIP_ID)).willReturn(false);

        assertThatThrownBy(() -> sut.confirm(ConfirmTripCommand.builder()
                .tripId(TRIP_ID).organizerId(10L).build()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DEPOSIT_UNPAID_EXISTS);

        then(saveDepositPolicyPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("이미 확정된 여행 재확정 시 → TRIP_ALREADY_CONFIRMED 예외")
    void confirm_alreadyConfirmed_throws() {
        DepositPolicy confirmed = DepositPolicy.builder()
                .id(1L).tripId(TRIP_ID).organizerId(10L)
                .depositAmount(Money.wons(50000)).status(PolicyStatus.CONFIRMED)
                .pgProvider(PgProvider.TOSS_PAYMENTS)
                .tripStartAt(LocalDateTime.now().plusDays(3))
                .createdAt(LocalDateTime.now()).build();

        given(loadDepositPolicyPort.findByTripId(TRIP_ID)).willReturn(Optional.of(confirmed));
        given(getTripDepositStatusUseCase.areAllDepositsPaid(TRIP_ID)).willReturn(true);

        assertThatThrownBy(() -> sut.confirm(ConfirmTripCommand.builder()
                .tripId(TRIP_ID).organizerId(10L).build()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TRIP_ALREADY_CONFIRMED);
    }

    @Test
    @DisplayName("정책이 없는 여행 확정 시도 → DEPOSIT_POLICY_NOT_FOUND 예외")
    void confirm_policyNotFound_throws() {
        given(loadDepositPolicyPort.findByTripId(TRIP_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.confirm(ConfirmTripCommand.builder()
                .tripId(TRIP_ID).organizerId(10L).build()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DEPOSIT_POLICY_NOT_FOUND);
    }
}
