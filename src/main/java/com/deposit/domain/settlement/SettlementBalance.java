package com.deposit.domain.settlement;

import com.deposit.domain.deposit.vo.Money;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 여행 내 특정 payer-debtor 쌍의 나중에 정산 누적 금액
 * DEFERRED 정산이 추가될 때마다 totalAmount가 증가한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementBalance {

    private Long id;
    private UUID tripId;
    private Long payerId;
    private Long debtorId;
    private Money totalAmount;
    private LocalDateTime lastUpdatedAt;

    @Builder
    private SettlementBalance(Long id, UUID tripId, Long payerId, Long debtorId,
                               Money totalAmount, LocalDateTime lastUpdatedAt) {
        this.id = id;
        this.tripId = tripId;
        this.payerId = payerId;
        this.debtorId = debtorId;
        this.totalAmount = totalAmount;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public static SettlementBalance create(UUID tripId, Long payerId, Long debtorId) {
        return SettlementBalance.builder()
                .tripId(tripId)
                .payerId(payerId)
                .debtorId(debtorId)
                .totalAmount(Money.ZERO)
                .lastUpdatedAt(LocalDateTime.now())
                .build();
    }

    public void accumulate(Money amount) {
        this.totalAmount = this.totalAmount.add(amount);
        this.lastUpdatedAt = LocalDateTime.now();
    }

    /**
     * 정산 금액 수정 시 기존 금액을 빼고 새 금액을 더해 잔액을 조정한다.
     */
    public void adjust(Money oldAmount, Money newAmount) {
        this.totalAmount = this.totalAmount.subtract(oldAmount).add(newAmount);
        this.lastUpdatedAt = LocalDateTime.now();
    }

    /**
     * 정산 취소 시 해당 금액을 누적 잔액에서 차감한다.
     */
    public void deduct(Money amount) {
        this.totalAmount = this.totalAmount.subtract(amount);
        this.lastUpdatedAt = LocalDateTime.now();
    }
}
