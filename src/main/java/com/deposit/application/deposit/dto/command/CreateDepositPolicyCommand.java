package com.deposit.application.deposit.dto.command;

import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.deposit.vo.PgProvider;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class CreateDepositPolicyCommand {
    private final UUID tripId;
    private final Long organizerId;
    private final Money depositAmount;
    private final PgProvider pgProvider;
    private final LocalDateTime tripStartAt;
}
