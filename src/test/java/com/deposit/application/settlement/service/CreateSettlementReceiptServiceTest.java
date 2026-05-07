package com.deposit.application.settlement.service;

import com.deposit.application.settlement.dto.command.CreateSettlementReceiptCommand;
import com.deposit.application.settlement.dto.result.SettlementReceiptResult;
import com.deposit.application.settlement.port.out.SaveSettlementReceiptPort;
import com.deposit.common.exception.BusinessException;
import com.deposit.common.exception.ErrorCode;
import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.settlement.SettlementReceipt;
import com.deposit.domain.settlement.vo.SettlementType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateSettlementReceiptService")
class CreateSettlementReceiptServiceTest {

    private static final UUID TRIP_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

    @Mock SaveSettlementReceiptPort saveSettlementReceiptPort;

    @InjectMocks
    CreateSettlementReceiptService sut;

    @Test
    @DisplayName("합계 == 총액인 경우 영수증 저장 성공")
    void create_validSum_saves() {
        given(saveSettlementReceiptPort.save(any())).willAnswer(inv -> inv.getArgument(0));

        SettlementReceiptResult result = sut.create(command(60000, List.of(
                item(10L, 20000), item(20L, 20000), item(30L, 20000)
        )));

        then(saveSettlementReceiptPort).should().save(any());
        assertThat(result.getTotalAmount().intValue()).isEqualTo(60000);
        assertThat(result.getItems()).hasSize(3);
    }

    @Test
    @DisplayName("합계 != 총액인 경우 → RECEIPT_ITEMS_TOTAL_MISMATCH 예외")
    void create_sumMismatch_throws() {
        assertThatThrownBy(() -> sut.create(command(60000, List.of(
                item(10L, 20000), item(20L, 15000) // 합계 35000 ≠ 60000
        ))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECEIPT_ITEMS_TOTAL_MISMATCH);

        then(saveSettlementReceiptPort).shouldHaveNoInteractions();
    }

    // ──────────────────────────────────────────────
    // 헬퍼
    // ──────────────────────────────────────────────

    private CreateSettlementReceiptCommand command(long total,
                                                    List<CreateSettlementReceiptCommand.ItemEntry> items) {
        return CreateSettlementReceiptCommand.builder()
                .tripId(TRIP_ID).payerId(1L)
                .totalAmount(Money.wons(total)).description("저녁")
                .type(SettlementType.IMMEDIATE).items(items)
                .build();
    }

    private CreateSettlementReceiptCommand.ItemEntry item(long debtorId, long amount) {
        return CreateSettlementReceiptCommand.ItemEntry.builder()
                .debtorId(debtorId).amount(Money.wons(amount)).build();
    }
}
