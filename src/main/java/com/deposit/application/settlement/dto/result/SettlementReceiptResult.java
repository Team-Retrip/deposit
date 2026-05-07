package com.deposit.application.settlement.dto.result;

import com.deposit.domain.settlement.SettlementReceipt;
import com.deposit.domain.settlement.vo.SettlementReceiptStatus;
import com.deposit.domain.settlement.vo.SettlementType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class SettlementReceiptResult {

    private Long id;
    private UUID tripId;
    private Long payerId;
    private BigDecimal totalAmount;
    private String description;
    private SettlementType type;
    private SettlementReceiptStatus status;
    private List<SettlementReceiptItemResult> items;
    private LocalDateTime createdAt;

    public static SettlementReceiptResult from(SettlementReceipt receipt) {
        return SettlementReceiptResult.builder()
                .id(receipt.getId())
                .tripId(receipt.getTripId())
                .payerId(receipt.getPayerId())
                .totalAmount(receipt.getTotalAmount().getAmount())
                .description(receipt.getDescription())
                .type(receipt.getType())
                .status(receipt.getStatus())
                .items(receipt.getItems().stream()
                        .map(SettlementReceiptItemResult::from)
                        .toList())
                .createdAt(receipt.getCreatedAt())
                .build();
    }
}
