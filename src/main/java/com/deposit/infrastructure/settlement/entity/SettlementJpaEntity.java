package com.deposit.infrastructure.settlement.entity;

import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.settlement.Settlement;
import com.deposit.domain.settlement.vo.SettlementStatus;
import com.deposit.domain.settlement.vo.SettlementType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "settlements",
        indexes = @Index(name = "idx_settlements_trip_id", columnList = "tripId"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID tripId;

    @Column(nullable = false)
    private Long payerId;

    @Column(nullable = false)
    private Long debtorId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public static SettlementJpaEntity from(Settlement settlement) {
        SettlementJpaEntity entity = new SettlementJpaEntity();
        entity.id = settlement.getId();
        entity.tripId = settlement.getTripId();
        entity.payerId = settlement.getPayerId();
        entity.debtorId = settlement.getDebtorId();
        entity.amount = settlement.getAmount().getAmount();
        entity.description = settlement.getDescription();
        entity.type = settlement.getType();
        entity.status = settlement.getStatus();
        entity.createdAt = settlement.getCreatedAt();
        return entity;
    }

    public Settlement toDomain() {
        return Settlement.builder()
                .id(id)
                .tripId(tripId)
                .payerId(payerId)
                .debtorId(debtorId)
                .amount(Money.of(amount))
                .description(description)
                .type(type)
                .status(status)
                .createdAt(createdAt)
                .build();
    }
}
