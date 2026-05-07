package com.deposit.infrastructure.settlement.entity;

import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.settlement.SettlementReceiptItem;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "settlement_receipt_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementReceiptItemJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_id", nullable = false)
    private SettlementReceiptJpaEntity receipt;

    @Column(nullable = false)
    private Long debtorId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    public static SettlementReceiptItemJpaEntity of(SettlementReceiptItem item,
                                                     SettlementReceiptJpaEntity receiptEntity) {
        SettlementReceiptItemJpaEntity entity = new SettlementReceiptItemJpaEntity();
        entity.id = item.getId();
        entity.receipt = receiptEntity;
        entity.debtorId = item.getDebtorId();
        entity.amount = item.getAmount().getAmount();
        return entity;
    }

    public SettlementReceiptItem toDomain() {
        return SettlementReceiptItem.builder()
                .id(id)
                .debtorId(debtorId)
                .amount(Money.of(amount))
                .build();
    }

    void updateAmount(BigDecimal newAmount) {
        this.amount = newAmount;
    }
}
