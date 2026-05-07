package com.deposit.application.settlement.dto.result;

import com.deposit.domain.settlement.SettlementReceiptItem;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class SettlementReceiptItemResult {

    private Long id;
    private Long debtorId;
    private BigDecimal amount;

    public static SettlementReceiptItemResult from(SettlementReceiptItem item) {
        return SettlementReceiptItemResult.builder()
                .id(item.getId())
                .debtorId(item.getDebtorId())
                .amount(item.getAmount().getAmount())
                .build();
    }
}
