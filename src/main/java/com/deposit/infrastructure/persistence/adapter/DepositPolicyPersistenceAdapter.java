package com.deposit.infrastructure.persistence.adapter;

import com.deposit.application.deposit.port.out.LoadDepositPolicyPort;
import com.deposit.application.deposit.port.out.SaveDepositPolicyPort;
import com.deposit.domain.deposit.DepositPolicy;
import com.deposit.domain.deposit.vo.PolicyStatus;
import com.deposit.infrastructure.persistence.entity.DepositPolicyJpaEntity;
import com.deposit.infrastructure.persistence.repository.DepositPolicyJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
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
    public List<DepositPolicy> findAllActiveExpiredBefore(LocalDateTime dateTime) {
        return repository.findByStatusAndTripStartAtBefore(PolicyStatus.ACTIVE, dateTime)
                .stream()
                .map(DepositPolicyJpaEntity::toDomain)
                .toList();
    }

    @Override
    public DepositPolicy save(DepositPolicy policy) {
        return repository.save(DepositPolicyJpaEntity.from(policy)).toDomain();
    }
}
