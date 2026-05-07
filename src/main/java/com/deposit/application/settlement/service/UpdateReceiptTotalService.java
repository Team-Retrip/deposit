package com.deposit.application.settlement.service;

import com.deposit.application.settlement.dto.command.UpdateReceiptTotalCommand;
import com.deposit.application.settlement.dto.result.SettlementReceiptResult;
import com.deposit.application.settlement.port.in.UpdateReceiptTotalUseCase;
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
public class UpdateReceiptTotalService implements UpdateReceiptTotalUseCase {

    private final LoadSettlementReceiptPort loadSettlementReceiptPort;
    private final SaveSettlementReceiptPort saveSettlementReceiptPort;

    @Override
    public SettlementReceiptResult updateTotal(UpdateReceiptTotalCommand command) {
        SettlementReceipt receipt = loadSettlementReceiptPort.findById(command.getReceiptId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RECEIPT_NOT_FOUND));

        if (receipt.isCancelled()) {
            throw new BusinessException(ErrorCode.RECEIPT_ALREADY_CANCELLED);
        }

        receipt.updateTotal(command.getNewTotal());

        return SettlementReceiptResult.from(saveSettlementReceiptPort.save(receipt));
    }
}
