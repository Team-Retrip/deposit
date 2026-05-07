package com.deposit.application.settlement.port.out;

import com.deposit.domain.deposit.vo.Money;

public interface SettlementNotificationPort {
    void notifySettlementCreated(Long debtorId, Long payerId, Money amount, String description);
    void notifySettlementAmountUpdated(Long debtorId, Long payerId, Money amount, String description);
    void notifySettlementCancelled(Long debtorId, Long payerId, Money amount, String description);
    void notifyEqualSplitSettlement(Long debtorId, Long payerId, Money amount, String description);
    void notifyIndividualSplitSettlement(Long debtorId, Long payerId, Money amount, String description);
}
