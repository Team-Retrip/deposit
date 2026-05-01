package com.deposit.infrastructure.persistence.entity;

import com.deposit.domain.deposit.Refund;
import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.deposit.vo.RefundStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "refunds")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefundJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long depositId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal requestedAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal pgFeeAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal refundedAmount;

    @Column(nullable = false)
    private boolean feeDeducted;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundStatus status;

    private String pgRefundTransactionId;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;

    public static RefundJpaEntity from(Refund refund) {
        RefundJpaEntity entity = new RefundJpaEntity();
        entity.id = refund.getId();
        entity.depositId = refund.getDepositId();
        entity.requestedAmount = refund.getRequestedAmount().getAmount();
        entity.pgFeeAmount = refund.getPgFeeAmount().getAmount();
        entity.refundedAmount = refund.getRefundedAmount().getAmount();
        entity.feeDeducted = refund.isFeeDeducted();
        entity.status = refund.getStatus();
        entity.pgRefundTransactionId = refund.getPgRefundTransactionId();
        entity.requestedAt = refund.getRequestedAt();
        entity.processedAt = refund.getProcessedAt();
        return entity;
    }

    public Refund toDomain() {
        return Refund.builder()
                .id(id)
                .depositId(depositId)
                .requestedAmount(Money.of(requestedAmount))
                .pgFeeAmount(Money.of(pgFeeAmount))
                .refundedAmount(Money.of(refundedAmount))
                .feeDeducted(feeDeducted)
                .status(status)
                .pgRefundTransactionId(pgRefundTransactionId)
                .requestedAt(requestedAt)
                .processedAt(processedAt)
                .build();
    }
}
