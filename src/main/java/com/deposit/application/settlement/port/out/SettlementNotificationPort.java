package com.deposit.application.settlement.port.out;

import com.deposit.domain.deposit.vo.Money;

public interface SettlementNotificationPort {
    void notifyImmediateSettlement(Long debtorId, Long payerId, Money amount, String description);
}
