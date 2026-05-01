package com.deposit.presentation.settlement;

import com.deposit.application.settlement.dto.command.CreateSettlementCommand;
import com.deposit.application.settlement.dto.result.SettlementBalanceResult;
import com.deposit.application.settlement.dto.result.SettlementResult;
import com.deposit.application.settlement.port.in.CreateSettlementUseCase;
import com.deposit.application.settlement.port.in.GetSettlementBalancesUseCase;
import com.deposit.application.settlement.port.in.GetTripSettlementsUseCase;
import com.deposit.common.response.ApiResponse;
import com.deposit.domain.deposit.vo.Money;
import com.deposit.presentation.settlement.dto.request.CreateSettlementRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/trips/{tripId}/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final CreateSettlementUseCase createSettlementUseCase;
    private final GetTripSettlementsUseCase getTripSettlementsUseCase;
    private final GetSettlementBalancesUseCase getSettlementBalancesUseCase;

    /**
     * 정산금 생성 (즉시 정산 or 나중에 정산)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SettlementResult> createSettlement(
            @PathVariable UUID tripId,
            @RequestBody @Valid CreateSettlementRequest request) {

        SettlementResult result = createSettlementUseCase.create(
                CreateSettlementCommand.builder()
                        .tripId(tripId)
                        .payerId(request.getPayerId())
                        .debtorId(request.getDebtorId())
                        .amount(Money.of(request.getAmount()))
                        .description(request.getDescription())
                        .type(request.getType())
                        .build()
        );

        String message = result.getNotificationMessage() != null
                ? result.getNotificationMessage()
                : String.format("정산금 %s원이 누적되었습니다.", result.getAmount());

        return ApiResponse.ok(result, message);
    }

    /**
     * 여행의 전체 정산 내역 조회
     */
    @GetMapping
    public ApiResponse<List<SettlementResult>> getSettlements(@PathVariable UUID tripId) {
        return ApiResponse.ok(getTripSettlementsUseCase.getByTrip(tripId));
    }

    /**
     * 여행의 나중에 정산 누적 잔액 전체 조회
     */
    @GetMapping("/balances")
    public ApiResponse<List<SettlementBalanceResult>> getBalances(@PathVariable UUID tripId) {
        return ApiResponse.ok(getSettlementBalancesUseCase.getBalancesByTrip(tripId));
    }

    /**
     * 특정 사용자(debtor)의 누적 정산 잔액 조회
     */
    @GetMapping("/balances/debtor/{debtorId}")
    public ApiResponse<List<SettlementBalanceResult>> getBalancesByDebtor(
            @PathVariable UUID tripId,
            @PathVariable Long debtorId) {
        return ApiResponse.ok(getSettlementBalancesUseCase.getBalancesByTripAndDebtor(tripId, debtorId));
    }
}
