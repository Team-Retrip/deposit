package com.deposit.application.settlement.dto.command;

import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.settlement.vo.SettlementType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class SplitSettlementCommand {
    private final UUID tripId;
    private final Long payerId;
    private final List<Long> debtorIds;
    private final Money totalAmount;
    private final String description;
    private final SettlementType type;
}
