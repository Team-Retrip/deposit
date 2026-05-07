package com.deposit.presentation.settlement;

import com.deposit.application.settlement.dto.command.CreateSettlementCommand;
import com.deposit.application.settlement.dto.command.CustomSplitSettlementCommand;
import com.deposit.application.settlement.dto.command.SplitSettlementCommand;
import com.deposit.application.settlement.dto.command.UpdateSettlementCommand;
import com.deposit.application.settlement.dto.result.SettlementBalanceResult;
import com.deposit.application.settlement.dto.result.SettlementResult;
import com.deposit.application.settlement.dto.result.SplitSettlementResult;
import com.deposit.application.settlement.dto.command.CancelSettlementCommand;
import com.deposit.application.settlement.dto.result.SettlementSummaryResult;
import com.deposit.application.settlement.port.in.CancelSettlementUseCase;
import com.deposit.application.settlement.port.in.CompleteSettlementUseCase;
import com.deposit.application.settlement.port.in.CreateSettlementUseCase;
import com.deposit.application.settlement.port.in.CustomSplitSettlementUseCase;
import com.deposit.application.settlement.port.in.GetSettlementBalancesUseCase;
import com.deposit.application.settlement.port.in.GetTripSettlementSummaryUseCase;
import com.deposit.application.settlement.port.in.GetTripSettlementsUseCase;
import com.deposit.application.settlement.port.in.SplitSettlementUseCase;
import com.deposit.application.settlement.port.in.UpdateSettlementUseCase;
import com.deposit.common.response.ApiResponse;
import com.deposit.domain.deposit.vo.Money;
import com.deposit.presentation.settlement.dto.request.CreateSettlementRequest;
import com.deposit.presentation.settlement.dto.request.CustomSplitSettlementRequest;
import com.deposit.presentation.settlement.dto.request.SplitSettlementRequest;
import com.deposit.presentation.settlement.dto.request.UpdateSettlementRequest;
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
    private final SplitSettlementUseCase splitSettlementUseCase;
    private final CustomSplitSettlementUseCase customSplitSettlementUseCase;
    private final UpdateSettlementUseCase updateSettlementUseCase;
    private final CancelSettlementUseCase cancelSettlementUseCase;
    private final CompleteSettlementUseCase completeSettlementUseCase;
    private final GetTripSettlementsUseCase getTripSettlementsUseCase;
    private final GetSettlementBalancesUseCase getSettlementBalancesUseCase;
    private final GetTripSettlementSummaryUseCase getTripSettlementSummaryUseCase;

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
     * 1/N 균등 분할 정산 요청
     * 선택한 채무자들에게 totalAmount를 N등분하여 알림 발송
     * 나머지(원 단위 절사)는 첫 번째 채무자에게 추가
     */
    @PostMapping("/split")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SplitSettlementResult> splitSettlement(
            @PathVariable UUID tripId,
            @RequestBody @Valid SplitSettlementRequest request) {

        SplitSettlementResult result = splitSettlementUseCase.split(
                SplitSettlementCommand.builder()
                        .tripId(tripId)
                        .payerId(request.getPayerId())
                        .debtorIds(request.getDebtorIds())
                        .totalAmount(Money.of(request.getTotalAmount()))
                        .description(request.getDescription())
                        .type(request.getType())
                        .build()
        );

        String message = result.getNotificationMessage() != null
                ? result.getNotificationMessage()
                : String.format("%d명에게 각 %s원씩 정산이 누적되었습니다.",
                result.getTotalDebtors(), result.getBaseAmount());

        return ApiResponse.ok(result, message);
    }

    /**
     * 개별 금액 정산 요청
     * 사용자별로 다른 금액을 지정하며, 합계 == totalAmount 이어야 함
     */
    @PostMapping("/split/custom")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SplitSettlementResult> customSplitSettlement(
            @PathVariable UUID tripId,
            @RequestBody @Valid CustomSplitSettlementRequest request) {

        List<CustomSplitSettlementCommand.DebtorAmount> debtorAmounts = request.getDebtorAmounts().stream()
                .map(d -> CustomSplitSettlementCommand.DebtorAmount.builder()
                        .debtorId(d.getDebtorId())
                        .amount(Money.of(d.getAmount()))
                        .build())
                .toList();

        SplitSettlementResult result = customSplitSettlementUseCase.split(
                CustomSplitSettlementCommand.builder()
                        .tripId(tripId)
                        .payerId(request.getPayerId())
                        .totalAmount(Money.of(request.getTotalAmount()))
                        .description(request.getDescription())
                        .type(request.getType())
                        .debtorAmounts(debtorAmounts)
                        .build()
        );

        String message = result.getNotificationMessage() != null
                ? result.getNotificationMessage()
                : String.format("%d명에게 개별 금액으로 정산이 누적되었습니다.", result.getTotalDebtors());

        return ApiResponse.ok(result, message);
    }

    /**
     * 정산금 금액 수정
     * 즉시 정산: 수정된 금액으로 알림 재발송
     * 나중에 정산: 누적 잔액 조정
     */
    @PatchMapping("/{settlementId}")
    public ApiResponse<SettlementResult> updateSettlement(
            @PathVariable UUID tripId,
            @PathVariable Long settlementId,
            @RequestBody @Valid UpdateSettlementRequest request) {

        SettlementResult result = updateSettlementUseCase.update(
                UpdateSettlementCommand.builder()
                        .settlementId(settlementId)
                        .newAmount(Money.of(request.getAmount()))
                        .build()
        );

        String message = result.getNotificationMessage() != null
                ? result.getNotificationMessage()
                : String.format("정산금이 %s원으로 수정되었습니다.", result.getAmount());

        return ApiResponse.ok(result, message);
    }

    /**
     * 정산 완료 처리 — 요청자(채권자)가 채무자의 지급을 확인 후 완료 처리
     * NOTIFIED, ACCUMULATED 상태만 완료 처리 가능
     */
    @PostMapping("/{settlementId}/complete")
    public ApiResponse<SettlementResult> completeSettlement(
            @PathVariable UUID tripId,
            @PathVariable Long settlementId) {

        SettlementResult result = completeSettlementUseCase.complete(settlementId);
        return ApiResponse.ok(result, "정산이 완료 처리되었습니다.");
    }

    /**
     * 정산 취소
     * 즉시 정산: 채무자에게 취소 알림 발송
     * 나중에 정산: 누적 잔액에서 해당 금액 차감
     */
    @DeleteMapping("/{settlementId}")
    public ApiResponse<SettlementResult> cancelSettlement(
            @PathVariable UUID tripId,
            @PathVariable Long settlementId) {

        SettlementResult result = cancelSettlementUseCase.cancel(
                CancelSettlementCommand.builder()
                        .settlementId(settlementId)
                        .build()
        );

        String message = result.getNotificationMessage() != null
                ? result.getNotificationMessage()
                : "정산이 취소되었습니다.";

        return ApiResponse.ok(result, message);
    }

    /**
     * 여행 정산 현황 요약 조회
     * - activeSettlements: 진행 중인 정산 요청 (수정/취소 가능)
     * - cancelledSettlements: 취소된 정산 내역 (수정 불가)
     * - pendingBalances: 개별 요청 없이 누적된 나중에 정산 금액
     */
    @GetMapping("/summary")
    public ApiResponse<SettlementSummaryResult> getSettlementSummary(@PathVariable UUID tripId) {
        return ApiResponse.ok(getTripSettlementSummaryUseCase.getSummary(tripId));
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
