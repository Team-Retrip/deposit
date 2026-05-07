package com.deposit.application.settlement.service;

import com.deposit.application.settlement.dto.command.UpdateReceiptTotalCommand;
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
@DisplayName("UpdateReceiptTotalService")
class UpdateReceiptTotalServiceTest {

    private static final UUID TRIP_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    private static final Long RECEIPT_ID = 1L;

    @Mock LoadSettlementReceiptPort loadSettlementReceiptPort;
    @Mock SaveSettlementReceiptPort saveSettlementReceiptPort;

    @InjectMocks
    UpdateReceiptTotalService sut;

    @Test
    @DisplayName("총액 증가 → 비례 배분, 합계 == 새 총액")
    void updateTotal_increase_proportionalRedistribution() {
        given(loadSettlementReceiptPort.findById(RECEIPT_ID)).willReturn(Optional.of(receipt60k()));
        given(saveSettlementReceiptPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        SettlementReceiptResult result = sut.updateTotal(
                UpdateReceiptTotalCommand.builder().receiptId(RECEIPT_ID).newTotal(Money.wons(90000)).build());

        assertThat(result.getTotalAmount().intValue()).isEqualTo(90000);
        int itemsSum = result.getItems().stream().mapToInt(i -> i.getAmount().intValue()).sum();
        assertThat(itemsSum).isEqualTo(90000);
    }

    @Test
    @DisplayName("존재하지 않는 영수증 → RECEIPT_NOT_FOUND 예외")
    void updateTotal_notFound_throws() {
        given(loadSettlementReceiptPort.findById(RECEIPT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.updateTotal(
                UpdateReceiptTotalCommand.builder().receiptId(RECEIPT_ID).newTotal(Money.wons(90000)).build()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECEIPT_NOT_FOUND);
    }

    @Test
    @DisplayName("취소된 영수증 총액 수정 → RECEIPT_ALREADY_CANCELLED 예외")
    void updateTotal_cancelled_throws() {
        SettlementReceipt cancelled = receipt60k();
        cancelled.cancel();
        given(loadSettlementReceiptPort.findById(RECEIPT_ID)).willReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> sut.updateTotal(
                UpdateReceiptTotalCommand.builder().receiptId(RECEIPT_ID).newTotal(Money.wons(90000)).build()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECEIPT_ALREADY_CANCELLED);
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
}
