package com.deposit.application.settlement.service;

import com.deposit.application.settlement.dto.result.SettlementBalanceResult;
import com.deposit.application.settlement.dto.result.SettlementResult;
import com.deposit.application.settlement.dto.result.SettlementSummaryResult;
import com.deposit.application.settlement.port.in.GetSettlementBalancesUseCase;
import com.deposit.application.settlement.port.in.GetTripSettlementSummaryUseCase;
import com.deposit.application.settlement.port.in.GetTripSettlementsUseCase;
import com.deposit.application.settlement.port.out.LoadSettlementBalancePort;
import com.deposit.application.settlement.port.out.LoadSettlementPort;
import com.deposit.domain.settlement.Settlement;
import com.deposit.domain.settlement.SettlementBalance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementQueryService implements GetTripSettlementsUseCase, GetSettlementBalancesUseCase, GetTripSettlementSummaryUseCase {

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

    @Override
    public SettlementSummaryResult getSummary(UUID tripId) {
        List<Settlement> settlements = loadSettlementPort.findAllByTripId(tripId);
        List<SettlementBalance> balances = loadSettlementBalancePort.findAllByTripId(tripId);
        return SettlementSummaryResult.of(settlements, balances);
    }
}
