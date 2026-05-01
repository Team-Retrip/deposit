package com.deposit.application.deposit.dto.command;

import com.deposit.domain.deposit.vo.PaymentMethod;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PayDepositCommand {
    private final Long depositId;
    private final Long userId;
    private final PaymentMethod paymentMethod;
}
