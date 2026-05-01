package com.deposit.application.deposit.dto.command;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class RequestDepositCommand {
    private final UUID tripId;
    private final Long organizerId;
    private final List<Long> userIds;
}
