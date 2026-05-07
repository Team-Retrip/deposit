package com.deposit.application.settlement.dto.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 1/N 균등 분할 정산 결과
 * - settlements: 생성된 개별 정산 목록
 * - baseAmount: 인원별 기본 금액 (floor)
 * - remainderAmount: 나머지 (첫 번째 채무자에게 추가, 0이면 미포함)
 * - totalDebtors: 총 채무자 수
 */
@Getter
@Builder
public class SplitSettlementResult {

    private List<SettlementResult> settlements;
    private BigDecimal baseAmount;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal remainderAmount;

    private int totalDebtors;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String notificationMessage;
}
