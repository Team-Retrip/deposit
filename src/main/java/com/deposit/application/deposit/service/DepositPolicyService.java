package com.deposit.application.deposit.service;

import com.deposit.application.deposit.dto.command.CreateDepositPolicyCommand;
import com.deposit.application.deposit.dto.command.RequestDepositCommand;
import com.deposit.application.deposit.dto.result.DepositPolicyResult;
import com.deposit.application.deposit.dto.result.DepositResult;
import com.deposit.application.deposit.port.in.CreateDepositPolicyUseCase;
import com.deposit.application.deposit.port.in.RequestDepositUseCase;
import com.deposit.application.deposit.port.out.LoadDepositPolicyPort;
import com.deposit.application.deposit.port.out.LoadDepositPort;
import com.deposit.application.deposit.port.out.SaveDepositPolicyPort;
import com.deposit.application.deposit.port.out.SaveDepositPort;
import com.deposit.common.exception.BusinessException;
import com.deposit.common.exception.ErrorCode;
import com.deposit.domain.deposit.Deposit;
import com.deposit.domain.deposit.DepositPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DepositPolicyService implements CreateDepositPolicyUseCase, RequestDepositUseCase {

    private final LoadDepositPolicyPort loadDepositPolicyPort;
    private final SaveDepositPolicyPort saveDepositPolicyPort;
    private final LoadDepositPort loadDepositPort;
    private final SaveDepositPort saveDepositPort;

    @Override
    public DepositPolicyResult createPolicy(CreateDepositPolicyCommand command) {
        if (loadDepositPolicyPort.findByTripId(command.getTripId()).isPresent()) {
            throw new BusinessException(ErrorCode.DEPOSIT_POLICY_ALREADY_EXISTS);
        }

        DepositPolicy policy = DepositPolicy.create(
                command.getTripId(),
                command.getOrganizerId(),
                command.getDepositAmount()
        );

        return DepositPolicyResult.from(saveDepositPolicyPort.save(policy));
    }

    @Override
    public List<DepositResult> requestDeposits(RequestDepositCommand command) {
        DepositPolicy policy = loadDepositPolicyPort.findByTripId(command.getTripId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPOSIT_POLICY_NOT_FOUND));

        List<DepositResult> results = new ArrayList<>();

        for (Long userId : command.getUserIds()) {
            if (loadDepositPort.existsByTripIdAndUserId(command.getTripId(), userId)) {
                throw new BusinessException(ErrorCode.DEPOSIT_ALREADY_EXISTS,
                        String.format("userId=%d 의 보증금 요청이 이미 존재합니다.", userId));
            }
            Deposit deposit = policy.issueDeposit(userId);
            results.add(DepositResult.from(saveDepositPort.save(deposit)));
        }

        return results;
    }
}
