package com.deposit.presentation.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class RefundDepositRequest {

    @NotNull(message = "사용자 ID를 입력해주세요.")
    private Long userId;
}
