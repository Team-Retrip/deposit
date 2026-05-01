package com.deposit.presentation.api.dto.request;

import com.deposit.domain.deposit.vo.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class PayDepositRequest {

    @NotNull(message = "사용자 ID를 입력해주세요.")
    private Long userId;

    @NotNull(message = "결제 수단을 선택해주세요. (CARD | BANK_TRANSFER)")
    private PaymentMethod paymentMethod;
}
