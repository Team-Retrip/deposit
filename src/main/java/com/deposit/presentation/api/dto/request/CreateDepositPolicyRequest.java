package com.deposit.presentation.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class CreateDepositPolicyRequest {

    @NotNull(message = "방장 ID를 입력해주세요.")
    private Long organizerId;

    @NotNull(message = "보증금 금액을 입력해주세요.")
    @DecimalMin(value = "1", message = "보증금은 1원 이상이어야 합니다.")
    private BigDecimal depositAmount;
}
