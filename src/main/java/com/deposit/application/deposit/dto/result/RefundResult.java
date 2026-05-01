package com.deposit.application.deposit.dto.result;

import com.deposit.domain.deposit.Refund;
import com.deposit.domain.deposit.vo.RefundStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class RefundResult {
    private Long id;
    private Long depositId;
    private BigDecimal requestedAmount;
    private BigDecimal pgFeeAmount;
    private BigDecimal refundedAmount;
    private boolean feeDeducted;
    private RefundStatus status;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;

    public static RefundResult from(Refund refund) {
        return RefundResult.builder()
                .id(refund.getId())
                .depositId(refund.getDepositId())
                .requestedAmount(refund.getRequestedAmount().getAmount())
                .pgFeeAmount(refund.getPgFeeAmount().getAmount())
                .refundedAmount(refund.getRefundedAmount().getAmount())
                .feeDeducted(refund.isFeeDeducted())
                .status(refund.getStatus())
                .requestedAt(refund.getRequestedAt())
                .processedAt(refund.getProcessedAt())
                .build();
    }
}
