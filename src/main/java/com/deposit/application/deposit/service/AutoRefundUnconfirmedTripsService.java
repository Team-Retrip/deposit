package com.deposit.application.deposit.service;

import com.deposit.application.deposit.dto.command.RefundDepositCommand;
import com.deposit.application.deposit.dto.result.AutoRefundResult;
import com.deposit.application.deposit.port.in.AutoRefundUnconfirmedTripsUseCase;
import com.deposit.application.deposit.port.in.RefundDepositUseCase;
import com.deposit.application.deposit.port.out.LoadDepositPolicyPort;
import com.deposit.application.deposit.port.out.LoadDepositPort;
import com.deposit.application.deposit.port.out.SaveDepositPolicyPort;
import com.deposit.domain.deposit.Deposit;
import com.deposit.domain.deposit.DepositPolicy;
import com.deposit.domain.deposit.vo.DepositStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AutoRefundUnconfirmedTripsService implements AutoRefundUnconfirmedTripsUseCase {

    private final LoadDepositPolicyPort loadDepositPolicyPort;
    private final SaveDepositPolicyPort saveDepositPolicyPort;
    private final LoadDepositPort loadDepositPort;
    private final RefundDepositUseCase refundDepositUseCase;

    @Override
    public AutoRefundResult autoRefundExpiredTrips() {
        List<DepositPolicy> expiredPolicies =
                loadDepositPolicyPort.findAllActiveExpiredBefore(LocalDateTime.now());

        int totalRefunded = 0;
        List<UUID> cancelledTripIds = new ArrayList<>();

        for (DepositPolicy policy : expiredPolicies) {
            log.info("[자동환불] 여행 {} 환불 처리 시작 (tripStartAt={})",
                    policy.getTripId(), policy.getTripStartAt());

            List<Deposit> deposits = loadDepositPort.findAllByTripId(policy.getTripId());
            for (Deposit deposit : deposits) {
                if (deposit.getStatus() == DepositStatus.PAID) {
                    refundDepositUseCase.refund(RefundDepositCommand.builder()
                            .depositId(deposit.getId())
                            .userId(deposit.getUserId())
                            .build());
                    totalRefunded++;
                    log.info("[자동환불] depositId={} userId={} 환불 완료",
                            deposit.getId(), deposit.getUserId());
                }
            }

            policy.cancel();
            saveDepositPolicyPort.save(policy);
            cancelledTripIds.add(policy.getTripId());

            log.info("[자동환불] 여행 {} 취소 완료", policy.getTripId());
        }

        return AutoRefundResult.builder()
                .processedTrips(expiredPolicies.size())
                .refundedDeposits(totalRefunded)
                .cancelledTripIds(cancelledTripIds)
                .build();
    }
}
