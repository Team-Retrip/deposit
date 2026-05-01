package com.deposit.infrastructure.persistence.entity;

import com.deposit.domain.deposit.DepositPolicy;
import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.deposit.vo.PolicyStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "deposit_policies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DepositPolicyJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, columnDefinition = "VARCHAR(36)")
    private UUID tripId;

    @Column(nullable = false)
    private Long organizerId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal depositAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public static DepositPolicyJpaEntity from(DepositPolicy policy) {
        DepositPolicyJpaEntity entity = new DepositPolicyJpaEntity();
        entity.id = policy.getId();
        entity.tripId = policy.getTripId();
        entity.organizerId = policy.getOrganizerId();
        entity.depositAmount = policy.getDepositAmount().getAmount();
        entity.status = policy.getStatus();
        entity.createdAt = policy.getCreatedAt();
        return entity;
    }

    public DepositPolicy toDomain() {
        return DepositPolicy.builder()
                .id(id)
                .tripId(tripId)
                .organizerId(organizerId)
                .depositAmount(Money.of(depositAmount))
                .status(status)
                .createdAt(createdAt)
                .build();
    }
}
