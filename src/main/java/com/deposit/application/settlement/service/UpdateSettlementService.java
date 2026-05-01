package com.deposit.application.settlement.service;

import com.deposit.application.settlement.dto.command.UpdateSettlementCommand;
import com.deposit.application.settlement.dto.result.SettlementResult;
import com.deposit.application.settlement.port.in.UpdateSettlementUseCase;
import com.deposit.application.settlement.port.out.LoadSettlementBalancePort;
import com.deposit.application.settlement.port.out.LoadSettlementPort;
import com.deposit.application.settlement.port.out.SaveSettlementBalancePort;
import com.deposit.application.settlement.port.out.SaveSettlementPort;
import com.deposit.application.settlement.port.out.SettlementNotificationPort;
import com.deposit.common.exception.BusinessException;
import com.deposit.common.exception.ErrorCode;
import com.deposit.domain.deposit.vo.Money;
import com.deposit.domain.settlement.Settlement;
import com.deposit.domain.settlement.SettlementBalance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateSettlementService implements UpdateSettlementUseCase {

    private final LoadSettlementPort loadSettlementPort;
    private final SaveSettlementPort saveSettlementPort;
    private final LoadSettlementBalancePort loadSettlementBalancePort;
    private final SaveSettlementBalancePort saveSettlementBalancePort;
    private final SettlementNotificationPort notificationPort;

    @Override
    public SettlementResult update(UpdateSettlementCommand command) {
        Settlement settlement = loadSettlementPort.findById(command.getSettlementId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

        Money oldAmount = settlement.getAmount();
        settlement.updateAmount(command.getNewAmount());

        String notificationMessage = null;

        if (settlement.isImmediate()) {
            notificationPort.notifyImmediateSettlement(
                    settlement.getDebtorId(),
                    settlement.getPayerId(),
                    command.getNewAmount(),
                    settlement.getDescription()
            );
            notificationMessage = String.format(
                    "userId=%d 에게 수정된 %s원 즉시 정산 알림이 발송되었습니다.",
                    settlement.getDebtorId(), command.getNewAmount()
            );
        } else {
            // DEFERRED: 누적 잔액 조정 (기존 금액 차감 후 새 금액 추가)
            SettlementBalance balance = loadSettlementBalancePort
                    .findByTripIdAndPayerIdAndDebtorId(
                            settlement.getTripId(), settlement.getPayerId(), settlement.getDebtorId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_BALANCE_NOT_FOUND));
            balance.adjust(oldAmount, command.getNewAmount());
            saveSettlementBalancePort.save(balance);
        }

        return SettlementResult.from(saveSettlementPort.save(settlement), notificationMessage);
    }
}
