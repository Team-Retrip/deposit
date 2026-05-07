package com.deposit.infrastructure.settlement.entity;

import com.deposit.domain.settlement.SettlementReceipt;
import com.deposit.domain.settlement.SettlementReceiptItem;
import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.settlement.vo.SettlementReceiptStatus;
import com.deposit.domain.settlement.vo.SettlementType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "settlement_receipts",
        indexes = @Index(name = "idx_settlement_receipts_trip_id", columnList = "tripId"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementReceiptJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID tripId;

    @Column(nullable = false)
    private Long payerId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementReceiptStatus status;

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SettlementReceiptItemJpaEntity> items = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    public static SettlementReceiptJpaEntity from(SettlementReceipt receipt) {
        SettlementReceiptJpaEntity entity = new SettlementReceiptJpaEntity();
        entity.id = receipt.getId();
        entity.tripId = receipt.getTripId();
        entity.payerId = receipt.getPayerId();
        entity.totalAmount = receipt.getTotalAmount().getAmount();
        entity.description = receipt.getDescription();
        entity.type = receipt.getType();
        entity.status = receipt.getStatus();
        entity.createdAt = receipt.getCreatedAt();

        entity.items.clear();
        for (SettlementReceiptItem item : receipt.getItems()) {
            entity.items.add(SettlementReceiptItemJpaEntity.of(item, entity));
        }
        return entity;
    }

    public SettlementReceipt toDomain() {
        List<SettlementReceiptItem> domainItems = items.stream()
                .map(SettlementReceiptItemJpaEntity::toDomain)
                .toList();

        return SettlementReceipt.builder()
                .id(id)
                .tripId(tripId)
                .payerId(payerId)
                .totalAmount(Money.of(totalAmount))
                .description(description)
                .type(type)
                .status(status)
                .items(domainItems)
                .createdAt(createdAt)
                .build();
    }
}
