package com.deposit.infrastructure.settlement.adapter;

import com.deposit.application.settlement.port.out.LoadSettlementBalancePort;
import com.deposit.application.settlement.port.out.SaveSettlementBalancePort;
import com.deposit.domain.settlement.SettlementBalance;
import com.deposit.infrastructure.settlement.entity.SettlementBalanceJpaEntity;
import com.deposit.infrastructure.settlement.repository.SettlementBalanceJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SettlementBalancePersistenceAdapter implements LoadSettlementBalancePort, SaveSettlementBalancePort {

    private final SettlementBalanceJpaRepository repository;

    @Override
    public Optional<SettlementBalance> findByTripIdAndPayerIdAndDebtorId(UUID tripId, Long payerId, Long debtorId) {
        return repository.findByTripIdAndPayerIdAndDebtorId(tripId, payerId, debtorId)
                .map(SettlementBalanceJpaEntity::toDomain);
    }

    @Override
    public List<SettlementBalance> findAllByTripId(UUID tripId) {
        return repository.findAllByTripId(tripId).stream()
                .map(SettlementBalanceJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<SettlementBalance> findAllByTripIdAndDebtorId(UUID tripId, Long debtorId) {
        return repository.findAllByTripIdAndDebtorId(tripId, debtorId).stream()
                .map(SettlementBalanceJpaEntity::toDomain)
                .toList();
    }

    @Override
    public SettlementBalance save(SettlementBalance balance) {
        return repository.save(SettlementBalanceJpaEntity.from(balance)).toDomain();
    }
}
