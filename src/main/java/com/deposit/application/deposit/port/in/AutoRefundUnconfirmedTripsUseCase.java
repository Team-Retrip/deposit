package com.deposit.application.deposit.port.in;

import com.deposit.application.deposit.dto.result.AutoRefundResult;

public interface AutoRefundUnconfirmedTripsUseCase {
    AutoRefundResult autoRefundExpiredTrips();
}
