package com.deposit.presentation.settlement;

import com.deposit.application.settlement.dto.command.CreateSettlementReceiptCommand;
import com.deposit.application.settlement.dto.command.UpdateReceiptItemsCommand;
import com.deposit.application.settlement.dto.command.UpdateReceiptTotalCommand;
import com.deposit.application.settlement.dto.result.SettlementReceiptResult;
import com.deposit.application.settlement.port.in.CancelSettlementReceiptUseCase;
import com.deposit.application.settlement.port.in.CreateSettlementReceiptUseCase;
import com.deposit.application.settlement.port.in.GetSettlementReceiptUseCase;
import com.deposit.application.settlement.port.in.UpdateReceiptItemsUseCase;
import com.deposit.application.settlement.port.in.UpdateReceiptTotalUseCase;
import com.deposit.common.response.ApiResponse;
import com.deposit.domain.deposit.vo.Money;
import com.deposit.presentation.settlement.dto.request.CreateSettlementReceiptRequest;
import com.deposit.presentation.settlement.dto.request.UpdateReceiptItemsRequest;
import com.deposit.presentation.settlement.dto.request.UpdateReceiptTotalRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/trips/{tripId}/settlement-receipts")
@RequiredArgsConstructor
public class SettlementReceiptController {

    private final CreateSettlementReceiptUseCase createSettlementReceiptUseCase;
    private final GetSettlementReceiptUseCase getSettlementReceiptUseCase;
    private final UpdateReceiptTotalUseCase updateReceiptTotalUseCase;
    private final UpdateReceiptItemsUseCase updateReceiptItemsUseCase;
    private final CancelSettlementReceiptUseCase cancelSettlementReceiptUseCase;

    /**
     * 정산 영수증 생성
     * 총액 == 인원별 금액 합계이어야 함
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SettlementReceiptResult> createReceipt(
            @PathVariable UUID tripId,
            @RequestBody @Valid CreateSettlementReceiptRequest request) {

        List<CreateSettlementReceiptCommand.ItemEntry> items = request.getItems().stream()
                .map(i -> CreateSettlementReceiptCommand.ItemEntry.builder()
                        .debtorId(i.getDebtorId())
                        .amount(Money.of(i.getAmount()))
                        .build())
                .toList();

        SettlementReceiptResult result = createSettlementReceiptUseCase.create(
                CreateSettlementReceiptCommand.builder()
                        .tripId(tripId)
                        .payerId(request.getPayerId())
                        .totalAmount(Money.of(request.getTotalAmount()))
                        .description(request.getDescription())
                        .type(request.getType())
                        .items(items)
                        .build()
        );

        return ApiResponse.ok(result, "정산 영수증이 생성되었습니다.");
    }

    /**
     * 여행의 정산 영수증 전체 목록 조회
     */
    @GetMapping
    public ApiResponse<List<SettlementReceiptResult>> getReceipts(@PathVariable UUID tripId) {
        return ApiResponse.ok(getSettlementReceiptUseCase.getByTrip(tripId));
    }

    /**
     * 정산 영수증 상세 조회 (영수증 + 인원별 금액)
     */
    @GetMapping("/{receiptId}")
    public ApiResponse<SettlementReceiptResult> getReceipt(
            @PathVariable UUID tripId,
            @PathVariable Long receiptId) {
        return ApiResponse.ok(getSettlementReceiptUseCase.getById(receiptId));
    }

    /**
     * 총액 수정 — 비례 배분으로 인원별 금액 자동 재계산
     * 인원별 금액은 직접 수정 불가
     */
    @PatchMapping("/{receiptId}/total")
    public ApiResponse<SettlementReceiptResult> updateTotal(
            @PathVariable UUID tripId,
            @PathVariable Long receiptId,
            @RequestBody @Valid UpdateReceiptTotalRequest request) {

        SettlementReceiptResult result = updateReceiptTotalUseCase.updateTotal(
                UpdateReceiptTotalCommand.builder()
                        .receiptId(receiptId)
                        .newTotal(Money.of(request.getTotalAmount()))
                        .build()
        );

        return ApiResponse.ok(result, "총액이 수정되었습니다. 인원별 금액이 비례 배분되었습니다.");
    }

    /**
     * 인원별 금액 수정 — 수정 후 합계 == 총액 이어야 함
     * 총액은 직접 수정 불가
     */
    @PatchMapping("/{receiptId}/items")
    public ApiResponse<SettlementReceiptResult> updateItems(
            @PathVariable UUID tripId,
            @PathVariable Long receiptId,
            @RequestBody @Valid UpdateReceiptItemsRequest request) {

        List<UpdateReceiptItemsCommand.ItemEntry> items = request.getItems().stream()
                .map(i -> UpdateReceiptItemsCommand.ItemEntry.builder()
                        .debtorId(i.getDebtorId())
                        .amount(Money.of(i.getAmount()))
                        .build())
                .toList();

        SettlementReceiptResult result = updateReceiptItemsUseCase.updateItems(
                UpdateReceiptItemsCommand.builder()
                        .receiptId(receiptId)
                        .items(items)
                        .build()
        );

        return ApiResponse.ok(result, "인원별 금액이 수정되었습니다.");
    }

    /**
     * 정산 영수증 취소
     */
    @DeleteMapping("/{receiptId}")
    public ApiResponse<SettlementReceiptResult> cancelReceipt(
            @PathVariable UUID tripId,
            @PathVariable Long receiptId) {

        SettlementReceiptResult result = cancelSettlementReceiptUseCase.cancel(receiptId);
        return ApiResponse.ok(result, "정산 영수증이 취소되었습니다.");
    }
}
