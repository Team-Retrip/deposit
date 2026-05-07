package com.deposit.presentation.settlement.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class UpdateReceiptTotalRequest {

    @NotNull
    @Positive
    private BigDecimal totalAmount;
}
