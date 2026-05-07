package com.deposit.application.settlement.service;

import com.deposit.application.settlement.dto.command.CreateSettlementReceiptCommand;
import com.deposit.application.settlement.dto.result.SettlementReceiptResult;
import com.deposit.application.settlement.port.in.CreateSettlementReceiptUseCase;
import com.deposit.application.settlement.port.out.SaveSettlementReceiptPort;
import com.deposit.common.exception.BusinessException;
import com.deposit.common.exception.ErrorCode;
import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.settlement.SettlementReceipt;
import com.deposit.domain.settlement.SettlementReceiptItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateSettlementReceiptService implements CreateSettlementReceiptUseCase {

    private final SaveSettlementReceiptPort saveSettlementReceiptPort;

    @Override
    public SettlementReceiptResult create(CreateSettlementReceiptCommand command) {
        List<SettlementReceiptItem> items = command.getItems().stream()
                .map(e -> SettlementReceiptItem.of(e.getDebtorId(), e.getAmount()))
                .toList();

        SettlementReceipt receipt = SettlementReceipt.create(
                command.getTripId(),
                command.getPayerId(),
                command.getTotalAmount(),
                command.getDescription(),
                command.getType(),
                items
        );

        if (!receipt.isItemsSumMatchesTotal()) {
            throw new BusinessException(ErrorCode.RECEIPT_ITEMS_TOTAL_MISMATCH);
        }

        return SettlementReceiptResult.from(saveSettlementReceiptPort.save(receipt));
    }
}
