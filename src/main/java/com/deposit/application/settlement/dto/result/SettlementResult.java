package com.deposit.application.settlement.dto.result;

import com.deposit.domain.settlement.Settlement;
import com.deposit.domain.settlement.vo.SettlementStatus;
import com.deposit.domain.settlement.vo.SettlementType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class SettlementResult {

    private Long id;
    private UUID tripId;
    private Long payerId;
    private Long debtorId;
    private BigDecimal amount;
    private String description;
    private SettlementType type;
    private SettlementStatus status;
    private LocalDateTime createdAt;

    /** 즉시 정산 시 알림 발송 안내 메시지 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String notificationMessage;

    public static SettlementResult from(Settlement settlement) {
        return from(settlement, null);
    }

    public static SettlementResult from(Settlement settlement, String notificationMessage) {
        return SettlementResult.builder()
                .id(settlement.getId())
                .tripId(settlement.getTripId())
                .payerId(settlement.getPayerId())
                .debtorId(settlement.getDebtorId())
                .amount(settlement.getAmount().getAmount())
                .description(settlement.getDescription())
                .type(settlement.getType())
                .status(settlement.getStatus())
                .createdAt(settlement.getCreatedAt())
                .notificationMessage(notificationMessage)
                .build();
    }
}
