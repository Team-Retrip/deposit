package com.deposit.infrastructure.notification;

import com.deposit.application.settlement.port.out.SettlementNotificationPort;
import com.deposit.domain.deposit.vo.Money;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class AlarmNotificationAdapter implements SettlementNotificationPort {

    private final RestClient restClient;
    private final String alarmServiceUrl;

    public AlarmNotificationAdapter(@Value("${alarm.service.url}") String alarmServiceUrl) {
        this.alarmServiceUrl = alarmServiceUrl;
        this.restClient = RestClient.create();
    }

    @Override
    public void notifySettlementCreated(Long debtorId, Long payerId, Money amount, String description) {
        sendAlarm(payerId, List.of(debtorId),
                Map.of("senderName", "사용자#" + payerId, "tripName", description),
                "SETTLEMENT_CREATE");
    }

    @Override
    public void notifySettlementAmountUpdated(Long debtorId, Long payerId, Money amount, String description) {
        sendAlarm(payerId, List.of(debtorId),
                Map.of("tripName", description, "amount", amount.getAmount().toPlainString()),
                "SETTLEMENT_AMOUNT_UPDATE");
    }

    @Override
    public void notifySettlementCancelled(Long debtorId, Long payerId, Money amount, String description) {
        sendAlarm(payerId, List.of(debtorId),
                Map.of("tripName", description),
                "SETTLEMENT_CANCEL");
    }

    @Override
    public void notifyEqualSplitSettlement(Long debtorId, Long payerId, Money amount, String description) {
        sendAlarm(payerId, List.of(debtorId),
                Map.of("senderName", "사용자#" + payerId, "tripName", description),
                "SETTLEMENT_EQUAL_SPLIT");
    }

    @Override
    public void notifyIndividualSplitSettlement(Long debtorId, Long payerId, Money amount, String description) {
        sendAlarm(payerId, List.of(debtorId),
                Map.of("senderName", "사용자#" + payerId, "tripName", description),
                "SETTLEMENT_INDIVIDUAL_SPLIT");
    }

    private void sendAlarm(Long senderId, List<Long> receiverIds, Map<String, String> parameters, String type) {
        try {
            AlarmRequest request = new AlarmRequest(
                    toUuid(senderId),
                    receiverIds.stream().map(this::toUuid).toList(),
                    parameters,
                    type
            );
            restClient.post()
                    .uri(alarmServiceUrl + "/alarms/send")
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
            log.debug("[알림] type={} sender={} receivers={}", type, senderId, receiverIds);
        } catch (Exception e) {
            log.warn("[알림] 발송 실패 type={} sender={} receivers={}: {}", type, senderId, receiverIds, e.getMessage());
        }
    }

    private UUID toUuid(Long userId) {
        return new UUID(0L, userId);
    }

    record AlarmRequest(
            UUID senderId,
            List<UUID> receiverIds,
            Map<String, String> parameters,
            String type
    ) {}
}
