package com.deposit.presentation.settlement.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class UpdateSettlementRequest {

    @NotNull(message = "수정할 금액을 입력해주세요.")
    @DecimalMin(value = "1", message = "정산 금액은 1원 이상이어야 합니다.")
    private BigDecimal amount;
}
