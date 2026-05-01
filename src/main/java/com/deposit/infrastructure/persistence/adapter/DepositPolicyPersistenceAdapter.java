package com.deposit.infrastructure.persistence.adapter;

import com.deposit.application.deposit.port.out.LoadDepositPolicyPort;
import com.deposit.application.deposit.port.out.SaveDepositPolicyPort;
import com.deposit.domain.deposit.DepositPolicy;
import com.deposit.infrastructure.persistence.entity.DepositPolicyJpaEntity;
import com.deposit.infrastructure.persistence.repository.DepositPolicyJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DepositPolicyPersistenceAdapter implements LoadDepositPolicyPort, SaveDepositPolicyPort {

    private final DepositPolicyJpaRepository repository;

    @Override
    public Optional<DepositPolicy> findByTripId(UUID tripId) {
        return repository.findByTripId(tripId).map(DepositPolicyJpaEntity::toDomain);
    }

    @Override
    public Optional<DepositPolicy> findById(Long policyId) {
        return repository.findById(policyId).map(DepositPolicyJpaEntity::toDomain);
    }

    @Override
    public DepositPolicy save(DepositPolicy policy) {
        return repository.save(DepositPolicyJpaEntity.from(policy)).toDomain();
    }
}
