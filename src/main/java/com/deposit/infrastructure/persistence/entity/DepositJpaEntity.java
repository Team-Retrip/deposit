package com.deposit.infrastructure.persistence.entity;

import com.deposit.domain.deposit.Deposit;
import com.deposit.domain.deposit.vo.DepositStatus;
import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.deposit.vo.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "deposits",
        indexes = @Index(name = "idx_deposits_trip_user", columnList = "tripId, userId"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DepositJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID tripId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DepositStatus status;

    private String pgTransactionId;

    @Column(precision = 10, scale = 6)
    private BigDecimal pgFeeRate;

    private LocalDateTime paidAt;
    private LocalDateTime refundDeadline;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public static DepositJpaEntity from(Deposit deposit) {
        DepositJpaEntity entity = new DepositJpaEntity();
        entity.id = deposit.getId();
        entity.tripId = deposit.getTripId();
        entity.userId = deposit.getUserId();
        entity.amount = deposit.getAmount().getAmount();
        entity.paymentMethod = deposit.getPaymentMethod();
        entity.status = deposit.getStatus();
        entity.pgTransactionId = deposit.getPgTransactionId();
        entity.pgFeeRate = deposit.getPgFeeRate();
        entity.paidAt = deposit.getPaidAt();
        entity.refundDeadline = deposit.getRefundDeadline();
        entity.createdAt = deposit.getCreatedAt();
        return entity;
    }

    public Deposit toDomain() {
        return Deposit.builder()
                .id(id)
                .tripId(tripId)
                .userId(userId)
                .amount(Money.of(amount))
                .paymentMethod(paymentMethod)
                .status(status)
                .pgTransactionId(pgTransactionId)
                .pgFeeRate(pgFeeRate)
                .paidAt(paidAt)
                .refundDeadline(refundDeadline)
                .createdAt(createdAt)
                .build();
    }
}
