package com.deposit.application.deposit.dto.command;

import com.deposit.domain.deposit.vo.Money;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class CreateDepositPolicyCommand {
    private final UUID tripId;
    private final Long organizerId;
    private final Money depositAmount;
}
