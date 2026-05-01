package com.deposit.infrastructure.settlement.entity;

import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.settlement.SettlementBalance;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "settlement_balances",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_balance_trip_payer_debtor",
                columnNames = {"tripId", "payerId", "debtorId"}
        ))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementBalanceJpaEntity {

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
    private BigDecimal totalAmount;

    private LocalDateTime lastUpdatedAt;

    public static SettlementBalanceJpaEntity from(SettlementBalance balance) {
        SettlementBalanceJpaEntity entity = new SettlementBalanceJpaEntity();
        entity.id = balance.getId();
        entity.tripId = balance.getTripId();
        entity.payerId = balance.getPayerId();
        entity.debtorId = balance.getDebtorId();
        entity.totalAmount = balance.getTotalAmount().getAmount();
        entity.lastUpdatedAt = balance.getLastUpdatedAt();
        return entity;
    }

    public SettlementBalance toDomain() {
        return SettlementBalance.builder()
                .id(id)
                .tripId(tripId)
                .payerId(payerId)
                .debtorId(debtorId)
                .totalAmount(Money.of(totalAmount))
                .lastUpdatedAt(lastUpdatedAt)
                .build();
    }
}
