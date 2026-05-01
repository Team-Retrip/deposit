package com.deposit.application.deposit.service;

import com.deposit.application.deposit.dto.result.DepositResult;
import com.deposit.application.deposit.dto.result.TripDepositStatusResult;
import com.deposit.application.deposit.port.in.GetDepositDetailUseCase;
import com.deposit.application.deposit.port.in.GetTripDepositStatusUseCase;
import com.deposit.application.deposit.port.out.LoadDepositPort;
import com.deposit.common.exception.BusinessException;
import com.deposit.common.exception.ErrorCode;
import com.deposit.domain.deposit.Deposit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepositQueryService implements GetTripDepositStatusUseCase, GetDepositDetailUseCase {

    private final LoadDepositPort loadDepositPort;

    @Override
    public TripDepositStatusResult getStatus(UUID tripId) {
        List<Deposit> deposits = loadDepositPort.findAllByTripId(tripId);

        long paidCount = deposits.stream().filter(Deposit::isPaid).count();
        int unpaidCount = deposits.size() - (int) paidCount;

        return TripDepositStatusResult.builder()
                .tripId(tripId)
                .totalCount(deposits.size())
                .paidCount((int) paidCount)
                .unpaidCount(unpaidCount)
                .allPaid(unpaidCount == 0 && !deposits.isEmpty())
                .deposits(deposits.stream().map(DepositResult::from).toList())
                .build();
    }

    @Override
    public boolean areAllDepositsPaid(UUID tripId) {
        List<Deposit> deposits = loadDepositPort.findAllByTripId(tripId);
        if (deposits.isEmpty()) return false;
        return deposits.stream().allMatch(Deposit::isPaid);
    }

    @Override
    public DepositResult getDeposit(Long depositId) {
        Deposit deposit = loadDepositPort.findById(depositId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPOSIT_NOT_FOUND));
        return DepositResult.from(deposit);
    }
}
