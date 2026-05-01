package com.deposit.application.settlement.service;

import com.deposit.application.settlement.dto.command.CreateSettlementCommand;
import com.deposit.application.settlement.dto.result.SettlementResult;
import com.deposit.application.settlement.port.in.CreateSettlementUseCase;
import com.deposit.application.settlement.port.out.LoadSettlementBalancePort;
import com.deposit.application.settlement.port.out.SaveSettlementBalancePort;
import com.deposit.application.settlement.port.out.SaveSettlementPort;
import com.deposit.application.settlement.port.out.SettlementNotificationPort;
import com.deposit.domain.settlement.Settlement;
import com.deposit.domain.settlement.SettlementBalance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateSettlementService implements CreateSettlementUseCase {

    private final SaveSettlementPort saveSettlementPort;
    private final LoadSettlementBalancePort loadSettlementBalancePort;
    private final SaveSettlementBalancePort saveSettlementBalancePort;
    private final SettlementNotificationPort notificationPort;

    @Override
    public SettlementResult create(CreateSettlementCommand command) {
        Settlement settlement = Settlement.create(
                command.getTripId(),
                command.getPayerId(),
                command.getDebtorId(),
                command.getAmount(),
                command.getDescription(),
                command.getType()
        );

        String notificationMessage = null;

        if (settlement.isImmediate()) {
            notificationPort.notifyImmediateSettlement(
                    command.getDebtorId(),
                    command.getPayerId(),
                    command.getAmount(),
                    command.getDescription()
            );
            settlement.notified();
            notificationMessage = String.format(
                    "userId=%d 에게 %s원 즉시 정산 알림이 발송되었습니다.",
                    command.getDebtorId(), command.getAmount()
            );
        } else {
            // DEFERRED: 누적 잔액에 추가
            SettlementBalance balance = loadSettlementBalancePort
                    .findByTripIdAndPayerIdAndDebtorId(
                            command.getTripId(), command.getPayerId(), command.getDebtorId())
                    .orElseGet(() -> SettlementBalance.create(
                            command.getTripId(), command.getPayerId(), command.getDebtorId()));
            balance.accumulate(command.getAmount());
            saveSettlementBalancePort.save(balance);
            settlement.accumulated();
        }

        return SettlementResult.from(saveSettlementPort.save(settlement), notificationMessage);
    }
}
