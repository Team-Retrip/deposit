package com.deposit.application.settlement.service;

import com.deposit.application.settlement.dto.command.CustomSplitSettlementCommand;
import com.deposit.application.settlement.dto.result.SettlementResult;
import com.deposit.application.settlement.dto.result.SplitSettlementResult;
import com.deposit.application.settlement.port.in.CustomSplitSettlementUseCase;
import com.deposit.application.settlement.port.out.LoadSettlementBalancePort;
import com.deposit.application.settlement.port.out.SaveSettlementBalancePort;
import com.deposit.application.settlement.port.out.SaveSettlementPort;
import com.deposit.application.settlement.port.out.SettlementNotificationPort;
import com.deposit.common.exception.BusinessException;
import com.deposit.common.exception.ErrorCode;
import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.settlement.Settlement;
import com.deposit.domain.settlement.SettlementBalance;
import com.deposit.domain.settlement.vo.SettlementType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomSplitSettlementService implements CustomSplitSettlementUseCase {

    private final SaveSettlementPort saveSettlementPort;
    private final LoadSettlementBalancePort loadSettlementBalancePort;
    private final SaveSettlementBalancePort saveSettlementBalancePort;
    private final SettlementNotificationPort notificationPort;

    @Override
    public SplitSettlementResult split(CustomSplitSettlementCommand command) {
        validateAmountsSum(command);

        List<SettlementResult> results = new ArrayList<>();
        boolean isImmediate = command.getType() == SettlementType.IMMEDIATE;
        String notificationMessage = null;

        for (CustomSplitSettlementCommand.DebtorAmount entry : command.getDebtorAmounts()) {
            Long debtorId = entry.getDebtorId();
            Money amount = entry.getAmount();

            Settlement settlement = Settlement.create(
                    command.getTripId(),
                    command.getPayerId(),
                    debtorId,
                    amount,
                    command.getDescription(),
                    command.getType()
            );

            if (isImmediate) {
                notificationPort.notifyIndividualSplitSettlement(
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
                    "%d명에게 개별 금액으로 정산 알림이 발송되었습니다.", command.getDebtorAmounts().size());
        }

        return SplitSettlementResult.builder()
                .settlements(results)
                .baseAmount(command.getTotalAmount().getAmount())
                .totalDebtors(command.getDebtorAmounts().size())
                .notificationMessage(notificationMessage)
                .build();
    }

    private void validateAmountsSum(CustomSplitSettlementCommand command) {
        Money sum = command.getDebtorAmounts().stream()
                .map(CustomSplitSettlementCommand.DebtorAmount::getAmount)
                .reduce(Money.ZERO, Money::add);

        if (!sum.equals(command.getTotalAmount())) {
            throw new BusinessException(ErrorCode.SETTLEMENT_CUSTOM_AMOUNTS_MISMATCH);
        }
    }
}
