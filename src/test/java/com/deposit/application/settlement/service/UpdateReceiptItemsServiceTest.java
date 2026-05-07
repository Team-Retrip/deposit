package com.deposit.application.settlement.service;

import com.deposit.application.settlement.dto.command.UpdateReceiptItemsCommand;
import com.deposit.application.settlement.dto.result.SettlementReceiptResult;
import com.deposit.application.settlement.port.out.LoadSettlementReceiptPort;
import com.deposit.application.settlement.port.out.SaveSettlementReceiptPort;
import com.deposit.common.exception.BusinessException;
import com.deposit.common.exception.ErrorCode;
import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.settlement.SettlementReceipt;
import com.deposit.domain.settlement.SettlementReceiptItem;
import com.deposit.domain.settlement.vo.SettlementType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateReceiptItemsService")
class UpdateReceiptItemsServiceTest {

    private static final UUID TRIP_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final Long RECEIPT_ID = 1L;

    @Mock LoadSettlementReceiptPort loadSettlementReceiptPort;
    @Mock SaveSettlementReceiptPort saveSettlementReceiptPort;

    @InjectMocks
    UpdateReceiptItemsService sut;

    @Test
    @DisplayName("수정 후 합계 == 총액 → 저장 성공")
    void updateItems_sumMatches_saves() {
        given(loadSettlementReceiptPort.findById(RECEIPT_ID)).willReturn(Optional.of(receipt60k()));
        given(saveSettlementReceiptPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        // A: 20k→30k, B: 20k→10k → 합계 60k 유지
        SettlementReceiptResult result = sut.updateItems(command(
                List.of(entry(10L, 30000), entry(20L, 10000))
        ));

        assertThat(result.getTotalAmount().intValue()).isEqualTo(60000);
        int sum = result.getItems().stream().mapToInt(i -> i.getAmount().intValue()).sum();
        assertThat(sum).isEqualTo(60000);
    }

    @Test
    @DisplayName("수정 후 합계 != 총액 → RECEIPT_ITEMS_TOTAL_MISMATCH 예외")
    void updateItems_sumMismatch_throws() {
        given(loadSettlementReceiptPort.findById(RECEIPT_ID)).willReturn(Optional.of(receipt60k()));

        // A만 50k로 변경 → 합계 90k ≠ 60k
        assertThatThrownBy(() -> sut.updateItems(command(List.of(entry(10L, 50000)))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECEIPT_ITEMS_TOTAL_MISMATCH);

        then(saveSettlementReceiptPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("존재하지 않는 채무자 수정 → RECEIPT_ITEM_NOT_FOUND 예외")
    void updateItems_unknownDebtor_throws() {
        given(loadSettlementReceiptPort.findById(RECEIPT_ID)).willReturn(Optional.of(receipt60k()));

        assertThatThrownBy(() -> sut.updateItems(command(List.of(entry(999L, 60000)))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECEIPT_ITEM_NOT_FOUND);
    }

    @Test
    @DisplayName("취소된 영수증 항목 수정 → RECEIPT_ALREADY_CANCELLED 예외")
    void updateItems_cancelled_throws() {
        SettlementReceipt cancelled = receipt60k();
        cancelled.cancel();
        given(loadSettlementReceiptPort.findById(RECEIPT_ID)).willReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> sut.updateItems(command(List.of(entry(10L, 30000), entry(20L, 10000)))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECEIPT_ALREADY_CANCELLED);
    }

    @Test
    @DisplayName("존재하지 않는 영수증 → RECEIPT_NOT_FOUND 예외")
    void updateItems_notFound_throws() {
        given(loadSettlementReceiptPort.findById(RECEIPT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.updateItems(command(List.of(entry(10L, 60000)))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECEIPT_NOT_FOUND);
    }

    // ──────────────────────────────────────────────
    // 헬퍼
    // ──────────────────────────────────────────────

    private SettlementReceipt receipt60k() {
        return SettlementReceipt.create(TRIP_ID, 1L, Money.wons(60000), "저녁", SettlementType.IMMEDIATE,
                List.of(
                        SettlementReceiptItem.of(10L, Money.wons(20000)),
                        SettlementReceiptItem.of(20L, Money.wons(20000)),
                        SettlementReceiptItem.of(30L, Money.wons(20000))
                ));
    }

    private UpdateReceiptItemsCommand command(List<UpdateReceiptItemsCommand.ItemEntry> items) {
        return UpdateReceiptItemsCommand.builder().receiptId(RECEIPT_ID).items(items).build();
    }

    private UpdateReceiptItemsCommand.ItemEntry entry(long debtorId, long amount) {
        return UpdateReceiptItemsCommand.ItemEntry.builder()
                .debtorId(debtorId).amount(Money.wons(amount)).build();
    }
}
