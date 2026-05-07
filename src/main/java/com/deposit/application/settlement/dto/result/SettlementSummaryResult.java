package com.deposit.application.settlement.dto.result;

import com.deposit.domain.settlement.Settlement;
import com.deposit.domain.settlement.SettlementBalance;
import com.deposit.domain.settlement.vo.SettlementStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 여행 정산 현황 요약
 * - activeSettlements: 진행 중인 정산 요청 (NOTIFIED, ACCUMULATED)
 * - completedSettlements: 완료된 정산 (COMPLETED) — 수정 불가
 * - cancelledSettlements: 취소된 정산 내역 (CANCELLED) — 수정 불가
 * - pendingBalances: 개별 요청 없이 누적된 나중에 정산 금액
 */
@Getter
@Builder
public class SettlementSummaryResult {

    private List<SettlementResult> activeSettlements;
    private List<SettlementResult> completedSettlements;
    private List<SettlementResult> cancelledSettlements;
    private List<SettlementBalanceResult> pendingBalances;

    public static SettlementSummaryResult of(List<Settlement> settlements, List<SettlementBalance> balances) {
        List<SettlementResult> active = settlements.stream()
                .filter(s -> s.getStatus() == SettlementStatus.NOTIFIED
                        || s.getStatus() == SettlementStatus.ACCUMULATED)
                .map(SettlementResult::from)
                .toList();

        List<SettlementResult> completed = settlements.stream()
                .filter(Settlement::isCompleted)
                .map(SettlementResult::from)
                .toList();

        List<SettlementResult> cancelled = settlements.stream()
                .filter(Settlement::isCancelled)
                .map(SettlementResult::from)
                .toList();

        List<SettlementBalanceResult> pendingBalanceResults = balances.stream()
                .map(SettlementBalanceResult::from)
                .toList();

        return SettlementSummaryResult.builder()
                .activeSettlements(active)
                .completedSettlements(completed)
                .cancelledSettlements(cancelled)
                .pendingBalances(pendingBalanceResults)
                .build();
    }
}
