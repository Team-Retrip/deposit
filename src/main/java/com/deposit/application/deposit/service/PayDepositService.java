package com.deposit.application.deposit.service;

import com.deposit.application.deposit.dto.command.PayDepositCommand;
import com.deposit.application.deposit.dto.result.DepositResult;
import com.deposit.application.deposit.port.in.PayDepositUseCase;
import com.deposit.application.deposit.port.out.LoadDepositPort;
import com.deposit.application.deposit.port.out.PgGatewayPort;
import com.deposit.application.deposit.port.out.SaveDepositPort;
import com.deposit.common.exception.BusinessException;
import com.deposit.common.exception.ErrorCode;
import com.deposit.domain.deposit.Deposit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PayDepositService implements PayDepositUseCase {

    private final LoadDepositPort loadDepositPort;
    private final SaveDepositPort saveDepositPort;
    private final PgGatewayPort pgGatewayPort;

    @Override
    public DepositResult pay(PayDepositCommand command) {
        Deposit deposit = loadDepositPort.findById(command.getDepositId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPOSIT_NOT_FOUND));

        BigDecimal feeRate = pgGatewayPort.getFeeRate(command.getPaymentMethod());

        PgGatewayPort.PayResult pgResult = pgGatewayPort.pay(new PgGatewayPort.PayCommand(
                UUID.randomUUID().toString(),
                command.getUserId(),
                deposit.getAmount().getAmount(),
                command.getPaymentMethod(),
                "여행 보증금"
        ));

        if (!pgResult.success()) {
            throw new BusinessException(ErrorCode.PG_PAYMENT_FAILED, pgResult.failureReason());
        }

        deposit.pay(pgResult.pgTransactionId(), feeRate, command.getPaymentMethod());

        return DepositResult.from(saveDepositPort.save(deposit));
    }
}
