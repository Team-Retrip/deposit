package com.deposit.domain.settlement;

import com.deposit.domain.deposit.vo.Money;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementReceiptItem {

    private Long id;
    private Long debtorId;
    private Money amount;

    @Builder
    private SettlementReceiptItem(Long id, Long debtorId, Money amount) {
        this.id = id;
        this.debtorId = debtorId;
        this.amount = amount;
    }

    public static SettlementReceiptItem of(Long debtorId, Money amount) {
        return SettlementReceiptItem.builder()
                .debtorId(debtorId)
                .amount(amount)
                .build();
    }

    void updateAmount(Money newAmount) {
        this.amount = newAmount;
    }
}
