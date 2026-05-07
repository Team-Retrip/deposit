package com.deposit.infrastructure.notification;

import com.deposit.application.settlement.port.out.SettlementNotificationPort;
import com.deposit.domain.deposit.vo.Money;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 정산 알림 Mock 구현체
 * 실제 환경에서는 FCM, 앱 푸시, SMS 등으로 교체
 */
@Slf4j
public class MockSettlementNotificationAdapter implements SettlementNotificationPort {

    @Override
    public void notifySettlementCreated(Long debtorId, Long payerId, Money amount, String description) {
        log.info("[알림-Mock] SETTLEMENT_CREATE userId={} → userId={}: \"{}\" {}원",
                payerId, debtorId, description, amount);
    }

    @Override
    public void notifySettlementAmountUpdated(Long debtorId, Long payerId, Money amount, String description) {
        log.info("[알림-Mock] SETTLEMENT_AMOUNT_UPDATE userId={} → userId={}: \"{}\" {}원",
                payerId, debtorId, description, amount);
    }

    @Override
    public void notifySettlementCancelled(Long debtorId, Long payerId, Money amount, String description) {
        log.info("[알림-Mock] SETTLEMENT_CANCEL userId={} → userId={}: \"{}\" {}원 취소됨",
                payerId, debtorId, description, amount);
    }

    @Override
    public void notifyEqualSplitSettlement(Long debtorId, Long payerId, Money amount, String description) {
        log.info("[알림-Mock] SETTLEMENT_EQUAL_SPLIT userId={} → userId={}: \"{}\" {}원",
                payerId, debtorId, description, amount);
    }

    @Override
    public void notifyIndividualSplitSettlement(Long debtorId, Long payerId, Money amount, String description) {
        log.info("[알림-Mock] SETTLEMENT_INDIVIDUAL_SPLIT userId={} → userId={}: \"{}\" {}원",
                payerId, debtorId, description, amount);
    }
}
