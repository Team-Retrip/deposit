package com.deposit.application.settlement.service;

import com.deposit.application.settlement.dto.result.SettlementBalanceResult;
import com.deposit.application.settlement.dto.result.SettlementResult;
import com.deposit.application.settlement.port.in.GetSettlementBalancesUseCase;
import com.deposit.application.settlement.port.in.GetTripSettlementsUseCase;
import com.deposit.application.settlement.port.out.LoadSettlementBalancePort;
import com.deposit.application.settlement.port.out.LoadSettlementPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementQueryService implements GetTripSettlementsUseCase, GetSettlementBalancesUseCase {

    private final LoadSettlementPort loadSettlementPort;
    private final LoadSettlementBalancePort loadSettlementBalancePort;

    @Override
    public List<SettlementResult> getByTrip(UUID tripId) {
        return loadSettlementPort.findAllByTripId(tripId).stream()
                .map(SettlementResult::from)
                .toList();
    }

    @Override
    public List<SettlementBalanceResult> getBalancesByTrip(UUID tripId) {
        return loadSettlementBalancePort.findAllByTripId(tripId).stream()
                .map(SettlementBalanceResult::from)
                .toList();
    }

    @Override
    public List<SettlementBalanceResult> getBalancesByTripAndDebtor(UUID tripId, Long debtorId) {
        return loadSettlementBalancePort.findAllByTripIdAndDebtorId(tripId, debtorId).stream()
                .map(SettlementBalanceResult::from)
                .toList();
    }
}
