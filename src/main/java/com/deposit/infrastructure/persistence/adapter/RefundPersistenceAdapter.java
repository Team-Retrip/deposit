package com.deposit.infrastructure.persistence.adapter;

import com.deposit.application.deposit.port.out.SaveRefundPort;
import com.deposit.domain.deposit.Refund;
import com.deposit.infrastructure.persistence.entity.RefundJpaEntity;
import com.deposit.infrastructure.persistence.repository.RefundJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RefundPersistenceAdapter implements SaveRefundPort {

    private final RefundJpaRepository repository;

    @Override
    public Refund save(Refund refund) {
        return repository.save(RefundJpaEntity.from(refund)).toDomain();
    }

    @Override
    public Optional<Refund> findByDepositId(Long depositId) {
        return repository.findByDepositId(depositId).map(RefundJpaEntity::toDomain);
    }
}
