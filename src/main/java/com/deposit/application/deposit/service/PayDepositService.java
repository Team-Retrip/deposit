package com.deposit.application.deposit.service;

import com.deposit.application.deposit.dto.command.PayDepositCommand;
import com.deposit.application.deposit.dto.result.DepositResult;
import com.deposit.application.deposit.port.in.PayDepositUseCase;
import com.deposit.application.deposit.port.out.LoadDepositPolicyPort;
import com.deposit.application.deposit.port.out.LoadDepositPort;
import com.deposit.application.deposit.port.out.PgGatewayPort;
import com.deposit.application.deposit.port.out.SaveDepositPort;
import com.deposit.common.exception.BusinessException;
import com.deposit.common.exception.ErrorCode;
import com.deposit.domain.deposit.Deposit;
import com.deposit.domain.deposit.DepositPolicy;
import com.deposit.domain.deposit.vo.PgProvider;
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
    private final LoadDepositPolicyPort loadDepositPolicyPort;
    private final SaveDepositPort saveDepositPort;
    private final PgGatewayPort pgGatewayPort;

    @Override
    public DepositResult pay(PayDepositCommand command) {
        Deposit deposit = loadDepositPort.findById(command.getDepositId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPOSIT_NOT_FOUND));

        // 여행 정책에서 지정한 PG사 사용
        PgProvider pgProvider = loadDepositPolicyPort.findByTripId(deposit.getTripId())
                .map(DepositPolicy::getPgProvider)
                .orElse(null);

        BigDecimal feeRate = pgGatewayPort.getFeeRate(command.getPaymentMethod());

        PgGatewayPort.PayResult pgResult = pgGatewayPort.pay(new PgGatewayPort.PayCommand(
                UUID.randomUUID().toString(),
                command.getUserId(),
                deposit.getAmount().getAmount(),
                command.getPaymentMethod(),
                "여행 보증금",
                pgProvider
        ));

        if (!pgResult.success()) {
            throw new BusinessException(ErrorCode.PG_PAYMENT_FAILED, pgResult.failureReason());
        }

        deposit.pay(pgResult.pgTransactionId(), feeRate, command.getPaymentMethod());

        return DepositResult.from(saveDepositPort.save(deposit));
    }
}
