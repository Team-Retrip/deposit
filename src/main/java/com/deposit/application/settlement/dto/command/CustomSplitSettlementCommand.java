package com.deposit.application.settlement.dto.command;

import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.settlement.vo.SettlementType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class CustomSplitSettlementCommand {

    private final UUID tripId;
    private final Long payerId;
    private final Money totalAmount;
    private final String description;
    private final SettlementType type;
    private final List<DebtorAmount> debtorAmounts;

    @Getter
    @Builder
    public static class DebtorAmount {
        private final Long debtorId;
        private final Money amount;
    }
}
