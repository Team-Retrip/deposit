package com.deposit.domain.settlement;

import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.settlement.vo.SettlementStatus;
import com.deposit.domain.settlement.vo.SettlementType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 정산 항목 — 한 사람이 다른 사람에게 정산해야 하는 논리적 금액
 * IMMEDIATE: 알림 발송 후 NOTIFIED
 * DEFERRED : SettlementBalance에 누적 후 ACCUMULATED
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement {

    private Long id;
    private UUID tripId;
    private Long payerId;     // 정산 받아야 할 사람 (먼저 지출한 사람)
    private Long debtorId;    // 정산 해야 할 사람 (빚진 사람)
    private Money amount;
    private String description;
    private SettlementType type;
    private SettlementStatus status;
    private LocalDateTime createdAt;

    @Builder
    private Settlement(Long id, UUID tripId, Long payerId, Long debtorId,
                       Money amount, String description, SettlementType type,
                       SettlementStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.tripId = tripId;
        this.payerId = payerId;
        this.debtorId = debtorId;
        this.amount = amount;
        this.description = description;
        this.type = type;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static Settlement create(UUID tripId, Long payerId, Long debtorId,
                                    Money amount, String description, SettlementType type) {
        return Settlement.builder()
                .tripId(tripId)
                .payerId(payerId)
                .debtorId(debtorId)
                .amount(amount)
                .description(description)
                .type(type)
                .status(SettlementStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public void notified() {
        this.status = SettlementStatus.NOTIFIED;
    }

    public void accumulated() {
        this.status = SettlementStatus.ACCUMULATED;
    }

    public boolean isImmediate() {
        return this.type == SettlementType.IMMEDIATE;
    }
}
