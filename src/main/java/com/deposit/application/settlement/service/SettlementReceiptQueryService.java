package com.deposit.application.settlement.service;

import com.deposit.application.settlement.dto.result.SettlementReceiptResult;
import com.deposit.application.settlement.port.in.GetSettlementReceiptUseCase;
import com.deposit.application.settlement.port.out.LoadSettlementReceiptPort;
import com.deposit.common.exception.BusinessException;
import com.deposit.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementReceiptQueryService implements GetSettlementReceiptUseCase {

    private final LoadSettlementReceiptPort loadSettlementReceiptPort;

    @Override
    public SettlementReceiptResult getById(Long receiptId) {
        return loadSettlementReceiptPort.findById(receiptId)
                .map(SettlementReceiptResult::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECEIPT_NOT_FOUND));
    }

    @Override
    public List<SettlementReceiptResult> getByTrip(UUID tripId) {
        return loadSettlementReceiptPort.findAllByTripId(tripId).stream()
                .map(SettlementReceiptResult::from)
                .toList();
    }
}
