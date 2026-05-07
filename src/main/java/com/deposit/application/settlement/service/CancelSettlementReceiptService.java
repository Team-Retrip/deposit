package com.deposit.application.settlement.service;

import com.deposit.application.settlement.dto.result.SettlementReceiptResult;
import com.deposit.application.settlement.port.in.CancelSettlementReceiptUseCase;
import com.deposit.application.settlement.port.out.LoadSettlementReceiptPort;
import com.deposit.application.settlement.port.out.SaveSettlementReceiptPort;
import com.deposit.common.exception.BusinessException;
import com.deposit.common.exception.ErrorCode;
import com.deposit.domain.settlement.SettlementReceipt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CancelSettlementReceiptService implements CancelSettlementReceiptUseCase {

    private final LoadSettlementReceiptPort loadSettlementReceiptPort;
    private final SaveSettlementReceiptPort saveSettlementReceiptPort;

    @Override
    public SettlementReceiptResult cancel(Long receiptId) {
        SettlementReceipt receipt = loadSettlementReceiptPort.findById(receiptId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RECEIPT_NOT_FOUND));

        if (receipt.isCancelled()) {
            throw new BusinessException(ErrorCode.RECEIPT_ALREADY_CANCELLED);
        }

        receipt.cancel();
        return SettlementReceiptResult.from(saveSettlementReceiptPort.save(receipt));
    }
}
