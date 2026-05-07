package com.deposit.infrastructure.settlement.adapter;

import com.deposit.application.settlement.port.out.LoadSettlementReceiptPort;
import com.deposit.application.settlement.port.out.SaveSettlementReceiptPort;
import com.deposit.domain.settlement.SettlementReceipt;
import com.deposit.infrastructure.settlement.entity.SettlementReceiptJpaEntity;
import com.deposit.infrastructure.settlement.repository.SettlementReceiptJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SettlementReceiptPersistenceAdapter implements LoadSettlementReceiptPort, SaveSettlementReceiptPort {

    private final SettlementReceiptJpaRepository repository;

    @Override
    public Optional<SettlementReceipt> findById(Long receiptId) {
        return repository.findById(receiptId).map(SettlementReceiptJpaEntity::toDomain);
    }

    @Override
    public List<SettlementReceipt> findAllByTripId(UUID tripId) {
        return repository.findAllByTripId(tripId).stream()
                .map(SettlementReceiptJpaEntity::toDomain)
                .toList();
    }

    @Override
    public SettlementReceipt save(SettlementReceipt receipt) {
        return repository.save(SettlementReceiptJpaEntity.from(receipt)).toDomain();
    }
}
