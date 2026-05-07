package com.deposit.application.settlement.service;

import com.deposit.application.settlement.dto.command.UpdateReceiptItemsCommand;
import com.deposit.application.settlement.dto.result.SettlementReceiptResult;
import com.deposit.application.settlement.port.in.UpdateReceiptItemsUseCase;
import com.deposit.application.settlement.port.out.LoadSettlementReceiptPort;
import com.deposit.application.settlement.port.out.SaveSettlementReceiptPort;
import com.deposit.common.exception.BusinessException;
import com.deposit.common.exception.ErrorCode;
import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.settlement.SettlementReceipt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateReceiptItemsService implements UpdateReceiptItemsUseCase {

    private final LoadSettlementReceiptPort loadSettlementReceiptPort;
    private final SaveSettlementReceiptPort saveSettlementReceiptPort;

    @Override
    public SettlementReceiptResult updateItems(UpdateReceiptItemsCommand command) {
        SettlementReceipt receipt = loadSettlementReceiptPort.findById(command.getReceiptId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RECEIPT_NOT_FOUND));

        if (receipt.isCancelled()) {
            throw new BusinessException(ErrorCode.RECEIPT_ALREADY_CANCELLED);
        }

        for (UpdateReceiptItemsCommand.ItemEntry entry : command.getItems()) {
            if (!receipt.hasDebtor(entry.getDebtorId())) {
                throw new BusinessException(ErrorCode.RECEIPT_ITEM_NOT_FOUND);
            }
        }

        Map<Long, Money> debtorAmounts = command.getItems().stream()
                .collect(Collectors.toMap(
                        UpdateReceiptItemsCommand.ItemEntry::getDebtorId,
                        UpdateReceiptItemsCommand.ItemEntry::getAmount
                ));

        receipt.updateItems(debtorAmounts);

        if (!receipt.isItemsSumMatchesTotal()) {
            throw new BusinessException(ErrorCode.RECEIPT_ITEMS_TOTAL_MISMATCH);
        }

        return SettlementReceiptResult.from(saveSettlementReceiptPort.save(receipt));
    }
}
