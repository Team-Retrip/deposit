package com.deposit.application.deposit.port.in;

import com.deposit.application.deposit.dto.command.ConfirmTripCommand;
import com.deposit.application.deposit.dto.result.ConfirmTripResult;

public interface ConfirmTripUseCase {
    ConfirmTripResult confirm(ConfirmTripCommand command);
}
