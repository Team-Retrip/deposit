package com.deposit.application.settlement.service;

import com.deposit.application.settlement.dto.result.SettlementResult;
import com.deposit.application.settlement.port.in.CompleteSettlementUseCase;
import com.deposit.application.settlement.port.out.LoadSettlementPort;
import com.deposit.application.settlement.port.out.SaveSettlementPort;
import com.deposit.common.exception.BusinessException;
import com.deposit.common.exception.ErrorCode;
import com.deposit.domain.settlement.Settlement;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CompleteSettlementService implements CompleteSettlementUseCase {

    private final LoadSettlementPort loadSettlementPort;
    private final SaveSettlementPort saveSettlementPort;

    @Override
    public SettlementResult complete(Long settlementId) {
        Settlement settlement = loadSettlementPort.findById(settlementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

        if (settlement.isCompleted()) {
            throw new BusinessException(ErrorCode.SETTLEMENT_ALREADY_COMPLETED);
        }

        if (!settlement.isCompletable()) {
            throw new BusinessException(ErrorCode.SETTLEMENT_NOT_COMPLETABLE);
        }

        settlement.complete();
        return SettlementResult.from(saveSettlementPort.save(settlement));
    }
}
