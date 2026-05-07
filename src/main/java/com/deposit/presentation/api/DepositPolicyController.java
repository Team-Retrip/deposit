package com.deposit.presentation.api;

import com.deposit.application.deposit.dto.command.ConfirmTripCommand;
import com.deposit.application.deposit.dto.command.CreateDepositPolicyCommand;
import com.deposit.application.deposit.dto.command.RequestDepositCommand;
import com.deposit.application.deposit.dto.result.AutoRefundResult;
import com.deposit.application.deposit.dto.result.ConfirmTripResult;
import com.deposit.application.deposit.dto.result.DepositPolicyResult;
import com.deposit.application.deposit.dto.result.DepositResult;
import com.deposit.application.deposit.dto.result.TripDepositStatusResult;
import com.deposit.application.deposit.port.in.AutoRefundUnconfirmedTripsUseCase;
import com.deposit.application.deposit.port.in.ConfirmTripUseCase;
import com.deposit.application.deposit.port.in.CreateDepositPolicyUseCase;
import com.deposit.application.deposit.port.in.GetTripDepositStatusUseCase;
import com.deposit.application.deposit.port.in.RequestDepositUseCase;
import com.deposit.common.response.ApiResponse;
import com.deposit.domain.deposit.vo.Money;
import com.deposit.presentation.api.dto.request.ConfirmTripRequest;
import com.deposit.presentation.api.dto.request.CreateDepositPolicyRequest;
import com.deposit.presentation.api.dto.request.RequestDepositRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/trips/{tripId}/deposit-policy")
@RequiredArgsConstructor
public class DepositPolicyController {

    private final CreateDepositPolicyUseCase createDepositPolicyUseCase;
    private final RequestDepositUseCase requestDepositUseCase;
    private final GetTripDepositStatusUseCase getTripDepositStatusUseCase;
    private final ConfirmTripUseCase confirmTripUseCase;
    private final AutoRefundUnconfirmedTripsUseCase autoRefundUnconfirmedTripsUseCase;

    /**
     * 방장이 여행 보증금 정책 설정 (PG사 및 여행 시작 시간 포함)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DepositPolicyResult> createPolicy(
            @PathVariable UUID tripId,
            @RequestBody @Valid CreateDepositPolicyRequest request) {

        DepositPolicyResult result = createDepositPolicyUseCase.createPolicy(
                CreateDepositPolicyCommand.builder()
                        .tripId(tripId)
                        .organizerId(request.getOrganizerId())
                        .depositAmount(Money.of(request.getDepositAmount()))
                        .pgProvider(request.getPgProvider())
                        .tripStartAt(request.getTripStartAt())
                        .build()
        );
        return ApiResponse.ok(result, "보증금 정책이 설정되었습니다.");
    }

    /**
     * 방장이 참가자에게 보증금 요청
     */
    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<List<DepositResult>> requestDeposits(
            @PathVariable UUID tripId,
            @RequestBody @Valid RequestDepositRequest request) {

        List<DepositResult> results = requestDepositUseCase.requestDeposits(
                RequestDepositCommand.builder()
                        .tripId(tripId)
                        .organizerId(request.getOrganizerId())
                        .userIds(request.getUserIds())
                        .build()
        );
        return ApiResponse.ok(results, String.format("%d명에게 보증금 납부 요청이 발송되었습니다.", results.size()));
    }

    /**
     * 여행 전체 보증금 납부 현황 조회
     */
    @GetMapping("/status")
    public ApiResponse<TripDepositStatusResult> getTripDepositStatus(@PathVariable UUID tripId) {
        TripDepositStatusResult result = getTripDepositStatusUseCase.getStatus(tripId);
        return ApiResponse.ok(result, result.getTripStartMessage());
    }

    /**
     * 여행 확정: 전원 납부 완료 확인 후 여행 확정 처리
     */
    @PostMapping("/confirm")
    public ApiResponse<ConfirmTripResult> confirmTrip(
            @PathVariable UUID tripId,
            @RequestBody @Valid ConfirmTripRequest request) {

        ConfirmTripResult result = confirmTripUseCase.confirm(
                ConfirmTripCommand.builder()
                        .tripId(tripId)
                        .organizerId(request.getOrganizerId())
                        .build()
        );
        return ApiResponse.ok(result,
                String.format("여행이 확정되었습니다. 납부 완료 인원: %d명", result.getConfirmedDeposits()));
    }

    /**
     * 만료 여행 자동 환불 수동 트리거 (운영/테스트용)
     */
    @PostMapping("/auto-refund")
    public ApiResponse<AutoRefundResult> triggerAutoRefund() {
        AutoRefundResult result = autoRefundUnconfirmedTripsUseCase.autoRefundExpiredTrips();
        return ApiResponse.ok(result, result.getSummaryMessage());
    }
}
