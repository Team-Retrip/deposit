package com.deposit.application.settlement.dto.result;

import com.deposit.domain.settlement.SettlementBalance;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class SettlementBalanceResult {

    private Long id;
    private UUID tripId;
    private Long payerId;
    private Long debtorId;
    private BigDecimal totalAmount;
    private LocalDateTime lastUpdatedAt;

    public static SettlementBalanceResult from(SettlementBalance balance) {
        return SettlementBalanceResult.builder()
                .id(balance.getId())
                .tripId(balance.getTripId())
                .payerId(balance.getPayerId())
                .debtorId(balance.getDebtorId())
                .totalAmount(balance.getTotalAmount().getAmount())
                .lastUpdatedAt(balance.getLastUpdatedAt())
                .build();
    }
}
