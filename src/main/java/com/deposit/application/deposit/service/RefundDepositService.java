package com.deposit.application.deposit.service;

import com.deposit.application.deposit.dto.command.RefundDepositCommand;
import com.deposit.application.deposit.dto.result.RefundResult;
import com.deposit.application.deposit.port.in.RefundDepositUseCase;
import com.deposit.application.deposit.port.out.LoadDepositPort;
import com.deposit.application.deposit.port.out.PgGatewayPort;
import com.deposit.application.deposit.port.out.SaveDepositPort;
import com.deposit.application.deposit.port.out.SaveRefundPort;
import com.deposit.common.exception.BusinessException;
import com.deposit.common.exception.ErrorCode;
import com.deposit.domain.deposit.Deposit;
import com.deposit.domain.deposit.Refund;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RefundDepositService implements RefundDepositUseCase {

    private final LoadDepositPort loadDepositPort;
    private final SaveDepositPort saveDepositPort;
    private final SaveRefundPort saveRefundPort;
    private final PgGatewayPort pgGatewayPort;

    @Override
    public RefundResult refund(RefundDepositCommand command) {
        Deposit deposit = loadDepositPort.findById(command.getDepositId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPOSIT_NOT_FOUND));

        deposit.requestRefund();

        Refund refund = Refund.create(deposit);

        PgGatewayPort.RefundResult pgResult = pgGatewayPort.refund(new PgGatewayPort.RefundCommand(
                deposit.getPgTransactionId(),
                refund.getRefundedAmount().getAmount(),
                refund.isFeeDeducted() ? "30일 초과 환불 (PG 수수료 공제)" : "환불"
        ));

        if (!pgResult.success()) {
            refund.fail();
            saveRefundPort.save(refund);
            throw new BusinessException(ErrorCode.PG_REFUND_FAILED, pgResult.failureReason());
        }

        refund.complete(pgResult.pgRefundTransactionId());
        deposit.completeRefund();

        saveDepositPort.save(deposit);
        return RefundResult.from(saveRefundPort.save(refund));
    }
}
