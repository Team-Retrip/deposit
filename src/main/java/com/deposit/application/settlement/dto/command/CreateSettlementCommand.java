package com.deposit.application.settlement.dto.command;

import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.settlement.vo.SettlementType;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class CreateSettlementCommand {
    private final UUID tripId;
    private final Long payerId;
    private final Long debtorId;
    private final Money amount;
    private final String description;
    private final SettlementType type;
}
