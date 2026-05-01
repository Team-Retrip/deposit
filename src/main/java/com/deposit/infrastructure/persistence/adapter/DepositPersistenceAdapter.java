package com.deposit.infrastructure.persistence.adapter;

import com.deposit.application.deposit.port.out.LoadDepositPort;
import com.deposit.application.deposit.port.out.SaveDepositPort;
import com.deposit.domain.deposit.Deposit;
import com.deposit.infrastructure.persistence.entity.DepositJpaEntity;
import com.deposit.infrastructure.persistence.repository.DepositJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DepositPersistenceAdapter implements LoadDepositPort, SaveDepositPort {

    private final DepositJpaRepository repository;

    @Override
    public Optional<Deposit> findById(Long depositId) {
        return repository.findById(depositId).map(DepositJpaEntity::toDomain);
    }

    @Override
    public Optional<Deposit> findByTripIdAndUserId(UUID tripId, Long userId) {
        return repository.findByTripIdAndUserId(tripId, userId).map(DepositJpaEntity::toDomain);
    }

    @Override
    public List<Deposit> findAllByTripId(UUID tripId) {
        return repository.findAllByTripId(tripId).stream()
                .map(DepositJpaEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsByTripIdAndUserId(UUID tripId, Long userId) {
        return repository.existsByTripIdAndUserId(tripId, userId);
    }

    @Override
    public Deposit save(Deposit deposit) {
        return repository.save(DepositJpaEntity.from(deposit)).toDomain();
    }
}
