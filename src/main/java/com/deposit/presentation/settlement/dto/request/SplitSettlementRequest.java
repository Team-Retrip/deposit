package com.deposit.presentation.settlement.dto.request;

import com.deposit.domain.settlement.vo.SettlementType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@NoArgsConstructor
public class SplitSettlementRequest {

    @NotNull
    private Long payerId;

    @NotEmpty
    private List<Long> debtorIds;

    @NotNull
    @Positive
    private BigDecimal totalAmount;

    @NotBlank
    private String description;

    @NotNull
    private SettlementType type;
}
