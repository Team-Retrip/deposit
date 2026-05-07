package com.deposit.application.settlement.service;

import com.deposit.application.settlement.dto.command.SplitSettlementCommand;
import com.deposit.application.settlement.dto.result.SettlementResult;
import com.deposit.application.settlement.dto.result.SplitSettlementResult;
import com.deposit.application.settlement.port.in.SplitSettlementUseCase;
import com.deposit.application.settlement.port.out.LoadSettlementBalancePort;
import com.deposit.application.settlement.port.out.SaveSettlementBalancePort;
import com.deposit.application.settlement.port.out.SaveSettlementPort;
import com.deposit.application.settlement.port.out.SettlementNotificationPort;
import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.settlement.Settlement;
import com.deposit.domain.settlement.SettlementBalance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SplitSettlementService implements SplitSettlementUseCase {

    private final SaveSettlementPort saveSettlementPort;
    private final LoadSettlementBalancePort loadSettlementBalancePort;
    private final SaveSettlementBalancePort saveSettlementBalancePort;
    private final SettlementNotificationPort notificationPort;

    @Override
    public SplitSettlementResult split(SplitSettlementCommand command) {
        int n = command.getDebtorIds().size();
        Money base = command.getTotalAmount().divide(n);
        Money remainder = command.getTotalAmount().subtract(base.multiply(java.math.BigDecimal.valueOf(n)));

        List<SettlementResult> results = new ArrayList<>();
        boolean isImmediate = command.getType() == com.deposit.domain.settlement.vo.SettlementType.IMMEDIATE;
        String notificationMessage = null;

        for (int i = 0; i < command.getDebtorIds().size(); i++) {
            Long debtorId = command.getDebtorIds().get(i);
            // 나머지는 첫 번째 채무자에게 추가
            Money amount = (i == 0) ? base.add(remainder) : base;

            Settlement settlement = Settlement.create(
                    command.getTripId(),
                    command.getPayerId(),
                    debtorId,
                    amount,
                    command.getDescription(),
                    command.getType()
            );

            if (isImmediate) {
                notificationPort.notifyEqualSplitSettlement(
                        debtorId, command.getPayerId(), amount, command.getDescription());
                settlement.notified();
            } else {
                SettlementBalance balance = loadSettlementBalancePort
                        .findByTripIdAndPayerIdAndDebtorId(command.getTripId(), command.getPayerId(), debtorId)
                        .orElseGet(() -> SettlementBalance.create(
                                command.getTripId(), command.getPayerId(), debtorId));
                balance.accumulate(amount);
                saveSettlementBalancePort.save(balance);
                settlement.accumulated();
            }

            results.add(SettlementResult.from(saveSettlementPort.save(settlement)));
        }

        if (isImmediate) {
            notificationMessage = String.format(
                    "%d명에게 각 %s원 1/N 정산 알림이 발송되었습니다.", n, base);
        }

        return SplitSettlementResult.builder()
                .settlements(results)
                .baseAmount(base.getAmount())
                .remainderAmount(remainder.getAmount().compareTo(java.math.BigDecimal.ZERO) > 0
                        ? remainder.getAmount() : null)
                .totalDebtors(n)
                .notificationMessage(notificationMessage)
                .build();
    }
}
