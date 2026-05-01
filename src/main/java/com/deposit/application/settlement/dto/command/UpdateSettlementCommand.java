package com.deposit.application.settlement.dto.command;

import com.deposit.domain.deposit.vo.Money;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateSettlementCommand {
    private final Long settlementId;
    private final Money newAmount;
}
