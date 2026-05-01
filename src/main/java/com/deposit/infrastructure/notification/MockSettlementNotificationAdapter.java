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
@Component
public class MockSettlementNotificationAdapter implements SettlementNotificationPort {

    @Override
    public void notifyImmediateSettlement(Long debtorId, Long payerId, Money amount, String description) {
        log.info("[알림] userId={} → userId={} 즉시 정산 알림: \"{}\" {}원",
                payerId, debtorId, description, amount);
    }
}
