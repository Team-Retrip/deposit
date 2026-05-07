package com.deposit.application.settlement.dto.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CancelSettlementCommand {
    private final Long settlementId;
}
