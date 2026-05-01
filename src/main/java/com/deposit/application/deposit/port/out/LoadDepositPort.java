package com.deposit.application.deposit.port.out;

import com.deposit.domain.deposit.Deposit;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadDepositPort {
    Optional<Deposit> findById(Long depositId);
    Optional<Deposit> findByTripIdAndUserId(UUID tripId, Long userId);
    List<Deposit> findAllByTripId(UUID tripId);
    boolean existsByTripIdAndUserId(UUID tripId, Long userId);
}
