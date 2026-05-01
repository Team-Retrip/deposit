package com.deposit.presentation.settlement.dto.request;

import com.deposit.domain.settlement.vo.SettlementType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class CreateSettlementRequest {

    @NotNull(message = "결제자 ID를 입력해주세요.")
    private Long payerId;

    @NotNull(message = "정산 대상자 ID를 입력해주세요.")
    private Long debtorId;

    @NotNull(message = "정산 금액을 입력해주세요.")
    @DecimalMin(value = "1", message = "정산 금액은 1원 이상이어야 합니다.")
    private BigDecimal amount;

    @NotBlank(message = "정산 항목 설명을 입력해주세요.")
    private String description;

    @NotNull(message = "정산 유형을 선택해주세요. (IMMEDIATE | DEFERRED)")
    private SettlementType type;
}
