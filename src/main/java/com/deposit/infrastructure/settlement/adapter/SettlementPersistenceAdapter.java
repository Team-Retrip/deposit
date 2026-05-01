package com.deposit.infrastructure.settlement.adapter;

import com.deposit.application.settlement.port.out.LoadSettlementPort;
import com.deposit.application.settlement.port.out.SaveSettlementPort;
import com.deposit.domain.settlement.Settlement;
import com.deposit.infrastructure.settlement.entity.SettlementJpaEntity;
import com.deposit.infrastructure.settlement.repository.SettlementJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SettlementPersistenceAdapter implements LoadSettlementPort, SaveSettlementPort {

    private final SettlementJpaRepository repository;

    @Override
    public Optional<Settlement> findById(Long settlementId) {
        return repository.findById(settlementId).map(SettlementJpaEntity::toDomain);
    }

    @Override
    public List<Settlement> findAllByTripId(UUID tripId) {
        return repository.findAllByTripId(tripId).stream()
                .map(SettlementJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Settlement save(Settlement settlement) {
        return repository.save(SettlementJpaEntity.from(settlement)).toDomain();
    }
}
