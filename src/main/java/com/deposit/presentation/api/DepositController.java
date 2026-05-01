package com.deposit.presentation.api;

import com.deposit.application.deposit.dto.command.PayDepositCommand;
import com.deposit.application.deposit.dto.command.RefundDepositCommand;
import com.deposit.application.deposit.dto.result.DepositResult;
import com.deposit.application.deposit.dto.result.RefundResult;
import com.deposit.application.deposit.port.in.GetDepositDetailUseCase;
import com.deposit.application.deposit.port.in.PayDepositUseCase;
import com.deposit.application.deposit.port.in.RefundDepositUseCase;
import com.deposit.common.response.ApiResponse;
import com.deposit.presentation.api.dto.request.PayDepositRequest;
import com.deposit.presentation.api.dto.request.RefundDepositRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/deposits")
@RequiredArgsConstructor
public class DepositController {

    private final PayDepositUseCase payDepositUseCase;
    private final RefundDepositUseCase refundDepositUseCase;
    private final GetDepositDetailUseCase getDepositDetailUseCase;

    /**
     * 보증금 상세 조회 (30일 환불 안내 포함)
     */
    @GetMapping("/{depositId}")
    public ApiResponse<DepositResult> getDeposit(@PathVariable Long depositId) {
        return ApiResponse.ok(getDepositDetailUseCase.getDeposit(depositId));
    }

    /**
     * 보증금 결제 (카드 / 계좌이체)
     * 응답에 30일 환불 안내 문구 포함
     */
    @PostMapping("/{depositId}/pay")
    public ApiResponse<DepositResult> pay(
            @PathVariable Long depositId,
            @RequestBody @Valid PayDepositRequest request) {

        DepositResult result = payDepositUseCase.pay(
                PayDepositCommand.builder()
                        .depositId(depositId)
                        .userId(request.getUserId())
                        .paymentMethod(request.getPaymentMethod())
                        .build()
        );
        return ApiResponse.ok(result, result.getRefundNotice());
    }

    /**
     * 보증금 환불 요청
     * 30일 초과 시 PG 수수료 공제 후 환불
     */
    @PostMapping("/{depositId}/refund")
    public ApiResponse<RefundResult> refund(
            @PathVariable Long depositId,
            @RequestBody @Valid RefundDepositRequest request) {

        RefundResult result = refundDepositUseCase.refund(
                RefundDepositCommand.builder()
                        .depositId(depositId)
                        .userId(request.getUserId())
                        .build()
        );

        String message = result.isFeeDeducted()
                ? String.format("환불 기한(30일)이 초과되어 PG 수수료 %s원이 공제된 %s원이 환불됩니다.",
                result.getPgFeeAmount(), result.getRefundedAmount())
                : String.format("%s원 전액 환불이 완료되었습니다.", result.getRefundedAmount());

        return ApiResponse.ok(result, message);
    }
}
