package com.deposit.application.deposit.port.in;

import com.deposit.application.deposit.dto.result.TripDepositStatusResult;

import java.util.UUID;

public interface GetTripDepositStatusUseCase {
    TripDepositStatusResult getStatus(UUID tripId);
    boolean areAllDepositsPaid(UUID tripId);
}
