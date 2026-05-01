package com.deposit.application.deposit.dto.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RefundDepositCommand {
    private final Long depositId;
    private final Long userId;
}
