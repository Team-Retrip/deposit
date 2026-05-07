package com.deposit.application.settlement.service;

import com.deposit.application.settlement.dto.command.CancelSettlementCommand;
import com.deposit.application.settlement.dto.result.SettlementResult;
import com.deposit.application.settlement.port.in.CancelSettlementUseCase;
import com.deposit.application.settlement.port.out.LoadSettlementBalancePort;
import com.deposit.application.settlement.port.out.LoadSettlementPort;
import com.deposit.application.settlement.port.out.SaveSettlementBalancePort;
import com.deposit.application.settlement.port.out.SaveSettlementPort;
import com.deposit.application.settlement.port.out.SettlementNotificationPort;
import com.deposit.common.exception.BusinessException;
import com.deposit.common.exception.ErrorCode;
import com.deposit.domain.settlement.Settlement;
import com.deposit.domain.settlement.SettlementBalance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CancelSettlementService implements CancelSettlementUseCase {

    private final LoadSettlementPort loadSettlementPort;
    private final SaveSettlementPort saveSettlementPort;
    private final LoadSettlementBalancePort loadSettlementBalancePort;
    private final SaveSettlementBalancePort saveSettlementBalancePort;
    private final SettlementNotificationPort notificationPort;

    @Override
    public SettlementResult cancel(CancelSettlementCommand command) {
        Settlement settlement = loadSettlementPort.findById(command.getSettlementId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

        if (settlement.isCancelled()) {
            throw new BusinessException(ErrorCode.SETTLEMENT_ALREADY_CANCELLED);
        }

        String notificationMessage = null;

        if (settlement.isImmediate()) {
            // 즉시 정산: 채무자에게 취소 알림 발송 (기존 알림 클릭 시 취소됐음을 인지할 수 있도록)
            notificationPort.notifySettlementCancelled(
                    settlement.getDebtorId(),
                    settlement.getPayerId(),
                    settlement.getAmount(),
                    settlement.getDescription()
            );
            notificationMessage = String.format(
                    "userId=%d 에게 %s원 즉시 정산 취소 알림이 발송되었습니다.",
                    settlement.getDebtorId(), settlement.getAmount()
            );
        } else {
            // 나중에 정산: 누적 잔액에서 해당 금액 차감
            SettlementBalance balance = loadSettlementBalancePort
                    .findByTripIdAndPayerIdAndDebtorId(
                            settlement.getTripId(), settlement.getPayerId(), settlement.getDebtorId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_BALANCE_NOT_FOUND));
            balance.deduct(settlement.getAmount());
            saveSettlementBalancePort.save(balance);
        }

        settlement.cancel();
        return SettlementResult.from(saveSettlementPort.save(settlement), notificationMessage);
    }
}
