package com.deposit.application.settlement.dto.command;

import com.deposit.domain.deposit.vo.Money;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UpdateReceiptItemsCommand {

    private final Long receiptId;
    private final List<ItemEntry> items;

    @Getter
    @Builder
    public static class ItemEntry {
        private final Long debtorId;
        private final Money amount;
    }
}
