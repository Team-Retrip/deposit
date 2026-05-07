package com.deposit.application.deposit.service;

import com.deposit.application.deposit.dto.command.ConfirmTripCommand;
import com.deposit.application.deposit.dto.result.ConfirmTripResult;
import com.deposit.application.deposit.dto.result.TripDepositStatusResult;
import com.deposit.application.deposit.port.in.ConfirmTripUseCase;
import com.deposit.application.deposit.port.in.GetTripDepositStatusUseCase;
import com.deposit.application.deposit.port.out.LoadDepositPolicyPort;
import com.deposit.application.deposit.port.out.SaveDepositPolicyPort;
import com.deposit.common.exception.BusinessException;
import com.deposit.common.exception.ErrorCode;
import com.deposit.domain.deposit.DepositPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ConfirmTripService implements ConfirmTripUseCase {

    private final LoadDepositPolicyPort loadDepositPolicyPort;
    private final SaveDepositPolicyPort saveDepositPolicyPort;
    private final GetTripDepositStatusUseCase getTripDepositStatusUseCase;

    @Override
    public ConfirmTripResult confirm(ConfirmTripCommand command) {
        DepositPolicy policy = loadDepositPolicyPort.findByTripId(command.getTripId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPOSIT_POLICY_NOT_FOUND));

        if (!getTripDepositStatusUseCase.areAllDepositsPaid(command.getTripId())) {
            throw new BusinessException(ErrorCode.DEPOSIT_UNPAID_EXISTS);
        }

        policy.confirm();  // 이미 확정이면 TRIP_ALREADY_CONFIRMED 예외
        DepositPolicy saved = saveDepositPolicyPort.save(policy);

        TripDepositStatusResult status = getTripDepositStatusUseCase.getStatus(command.getTripId());
        return ConfirmTripResult.of(saved, status.getPaidCount());
    }
}
