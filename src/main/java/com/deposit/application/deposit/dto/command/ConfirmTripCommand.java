package com.deposit.application.deposit.dto.command;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ConfirmTripCommand {
    private final UUID tripId;
    private final Long organizerId;
}
