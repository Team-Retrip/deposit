package com.retrip.deposit.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@Getter
public class DepositMember {

    @Id
    @Column(columnDefinition = "varbinary(16)")
    private UUID id;

    @Column(nullable = false)
    private UUID memberId;

    private long amount;
    private long totalAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "deposit_id",
            nullable = false,
            columnDefinition = "varbinary(16)",
            foreignKey = @ForeignKey(name = "fk_deposit_member_to_deposit")
    )
    private Deposit deposit;


    public DepositMember(Deposit deposit, UUID memberId, Long totalAmount) {
        this.id = UUID.randomUUID();
        this.deposit = deposit;
        this.memberId = memberId;
        this.totalAmount = totalAmount;
        this.amount = 0L;
    }

    public boolean isDepositPaid() {
        return amount == totalAmount;
    }
}
