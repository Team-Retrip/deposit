package com.deposit.domain.settlement;

import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.settlement.vo.SettlementReceiptStatus;
import com.deposit.domain.settlement.vo.SettlementType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 정산 영수증 — 하나의 지출을 여러 채무자에게 나눈 기록
 * 총액 수정: 비례 배분 재계산 (나머지는 마지막 인원에게)
 * 개별 수정: 수정 후 합계 == 총액 이어야 함
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementReceipt {

    private Long id;
    private UUID tripId;
    private Long payerId;
    private Money totalAmount;
    private String description;
    private SettlementType type;
    private SettlementReceiptStatus status;
    private List<SettlementReceiptItem> items;
    private LocalDateTime createdAt;

    @Builder
    private SettlementReceipt(Long id, UUID tripId, Long payerId, Money totalAmount,
                               String description, SettlementType type,
                               SettlementReceiptStatus status, List<SettlementReceiptItem> items,
                               LocalDateTime createdAt) {
        this.id = id;
        this.tripId = tripId;
        this.payerId = payerId;
        this.totalAmount = totalAmount;
        this.description = description;
        this.type = type;
        this.status = status;
        this.items = new ArrayList<>(items);
        this.createdAt = createdAt;
    }

    public static SettlementReceipt create(UUID tripId, Long payerId, Money totalAmount,
                                            String description, SettlementType type,
                                            List<SettlementReceiptItem> items) {
        return SettlementReceipt.builder()
                .tripId(tripId)
                .payerId(payerId)
                .totalAmount(totalAmount)
                .description(description)
                .type(type)
                .status(SettlementReceiptStatus.ACTIVE)
                .items(items)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 총액 수정 — 비례 배분으로 개별 금액 재계산.
     * 나머지(원 단위 버림 오차)는 마지막 인원에게 합산.
     */
    public void updateTotal(Money newTotal) {
        BigDecimal oldTotal = this.totalAmount.getAmount();

        Money allocated = Money.ZERO;
        for (int i = 0; i < items.size() - 1; i++) {
            BigDecimal ratio = items.get(i).getAmount().getAmount()
                    .divide(oldTotal, 10, RoundingMode.HALF_UP);
            Money proportional = newTotal.multiply(ratio);
            items.get(i).updateAmount(proportional);
            allocated = allocated.add(proportional);
        }
        items.get(items.size() - 1).updateAmount(newTotal.subtract(allocated));

        this.totalAmount = newTotal;
    }

    /**
     * 개별 금액 일괄 수정.
     * 전달된 map(debtorId → 새 금액)으로 해당 항목을 업데이트한 뒤
     * 합계 == 총액인지 도메인 레벨에서는 확인하지 않는다.
     * (유효성 검사는 서비스 레이어 책임)
     */
    public void updateItems(Map<Long, Money> debtorAmounts) {
        for (SettlementReceiptItem item : items) {
            Money newAmount = debtorAmounts.get(item.getDebtorId());
            if (newAmount != null) {
                item.updateAmount(newAmount);
            }
        }
    }

    /** 개별 금액 합계 */
    public Money getTotalItemsSum() {
        return items.stream()
                .map(SettlementReceiptItem::getAmount)
                .reduce(Money.ZERO, Money::add);
    }

    /** 합계 == 총액 여부 */
    public boolean isItemsSumMatchesTotal() {
        return getTotalItemsSum().equals(this.totalAmount);
    }

    public void cancel() {
        this.status = SettlementReceiptStatus.CANCELLED;
    }

    public boolean isCancelled() {
        return this.status == SettlementReceiptStatus.CANCELLED;
    }

    public boolean hasDebtor(Long debtorId) {
        return items.stream().anyMatch(item -> item.getDebtorId().equals(debtorId));
    }
}
