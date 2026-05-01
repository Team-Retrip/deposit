package com.deposit.application.deposit.service;

import com.deposit.application.deposit.dto.command.CreateDepositPolicyCommand;
import com.deposit.application.deposit.dto.command.RequestDepositCommand;
import com.deposit.application.deposit.dto.result.DepositPolicyResult;
import com.deposit.application.deposit.dto.result.DepositResult;
import com.deposit.application.deposit.port.out.LoadDepositPolicyPort;
import com.deposit.application.deposit.port.out.LoadDepositPort;
import com.deposit.application.deposit.port.out.SaveDepositPolicyPort;
import com.deposit.application.deposit.port.out.SaveDepositPort;
import com.deposit.common.exception.BusinessException;
import com.deposit.common.exception.ErrorCode;
import com.deposit.domain.deposit.Deposit;
import com.deposit.domain.deposit.DepositPolicy;
import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.deposit.vo.PolicyStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DepositPolicyService")
class DepositPolicyServiceTest {

    private static final UUID TRIP_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final UUID OTHER_TRIP_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440099");

    @Mock LoadDepositPolicyPort loadDepositPolicyPort;
    @Mock SaveDepositPolicyPort saveDepositPolicyPort;
    @Mock LoadDepositPort loadDepositPort;
    @Mock SaveDepositPort saveDepositPort;

    @InjectMocks
    DepositPolicyService sut;

    @Test
    @DisplayName("보증금 정책 생성 성공")
    void createPolicy_success() {
        given(loadDepositPolicyPort.findByTripId(TRIP_ID)).willReturn(Optional.empty());
        given(saveDepositPolicyPort.save(any())).willAnswer(inv -> {
            DepositPolicy p = inv.getArgument(0);
            return DepositPolicy.builder()
                    .id(1L).tripId(p.getTripId()).organizerId(p.getOrganizerId())
                    .depositAmount(p.getDepositAmount()).status(PolicyStatus.ACTIVE)
                    .createdAt(LocalDateTime.now()).build();
        });

        DepositPolicyResult result = sut.createPolicy(CreateDepositPolicyCommand.builder()
                .tripId(TRIP_ID).organizerId(10L).depositAmount(Money.wons(50000)).build());

        assertThat(result.getTripId()).isEqualTo(TRIP_ID);
        assertThat(result.getDepositAmount()).isEqualByComparingTo("50000");
        then(saveDepositPolicyPort).should().save(any());
    }

    @Test
    @DisplayName("이미 정책이 존재하는 여행에 재설정 시 예외")
    void createPolicy_alreadyExists_throws() {
        DepositPolicy existing = DepositPolicy.create(TRIP_ID, 10L, Money.wons(50000));
        given(loadDepositPolicyPort.findByTripId(TRIP_ID)).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> sut.createPolicy(CreateDepositPolicyCommand.builder()
                .tripId(TRIP_ID).organizerId(10L).depositAmount(Money.wons(50000)).build()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DEPOSIT_POLICY_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("방장이 참가자 3명에게 보증금 요청 → 3건 생성")
    void requestDeposits_success() {
        DepositPolicy policy = DepositPolicy.create(TRIP_ID, 10L, Money.wons(50000));
        given(loadDepositPolicyPort.findByTripId(TRIP_ID)).willReturn(Optional.of(policy));
        given(loadDepositPort.existsByTripIdAndUserId(any(), any())).willReturn(false);
        given(saveDepositPort.save(any())).willAnswer(inv -> {
            Deposit d = inv.getArgument(0);
            return Deposit.builder()
                    .id((long)(Math.random() * 1000))
                    .tripId(d.getTripId()).userId(d.getUserId())
                    .amount(d.getAmount()).status(d.getStatus())
                    .createdAt(LocalDateTime.now()).build();
        });

        List<DepositResult> results = sut.requestDeposits(RequestDepositCommand.builder()
                .tripId(TRIP_ID).organizerId(10L).userIds(List.of(101L, 102L, 103L)).build());

        assertThat(results).hasSize(3);
        then(saveDepositPort).should(times(3)).save(any());
    }

    @Test
    @DisplayName("보증금 정책 없는 여행에 요청 시 예외")
    void requestDeposits_policyNotFound_throws() {
        given(loadDepositPolicyPort.findByTripId(OTHER_TRIP_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.requestDeposits(RequestDepositCommand.builder()
                .tripId(OTHER_TRIP_ID).organizerId(10L).userIds(List.of(101L)).build()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DEPOSIT_POLICY_NOT_FOUND);
    }

    @Test
    @DisplayName("이미 요청된 사용자 재요청 시 예외")
    void requestDeposits_duplicate_throws() {
        DepositPolicy policy = DepositPolicy.create(TRIP_ID, 10L, Money.wons(50000));
        given(loadDepositPolicyPort.findByTripId(TRIP_ID)).willReturn(Optional.of(policy));
        given(loadDepositPort.existsByTripIdAndUserId(TRIP_ID, 101L)).willReturn(true);

        assertThatThrownBy(() -> sut.requestDeposits(RequestDepositCommand.builder()
                .tripId(TRIP_ID).organizerId(10L).userIds(List.of(101L)).build()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DEPOSIT_ALREADY_EXISTS);
    }
}
