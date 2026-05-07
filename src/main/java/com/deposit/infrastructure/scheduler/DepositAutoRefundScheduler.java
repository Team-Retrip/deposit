package com.deposit.infrastructure.scheduler;

import com.deposit.application.deposit.dto.result.AutoRefundResult;
import com.deposit.application.deposit.port.in.AutoRefundUnconfirmedTripsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DepositAutoRefundScheduler {

    private final AutoRefundUnconfirmedTripsUseCase autoRefundUseCase;

    /** 매 1분마다 여행 시작 시간이 지났으나 미확정인 여행의 보증금 자동 환불 */
    @Scheduled(fixedDelay = 60_000)
    public void autoRefundExpiredTrips() {
        log.info("[자동환불 스케줄러] 만료 여행 자동 환불 체크 시작");
        AutoRefundResult result = autoRefundUseCase.autoRefundExpiredTrips();
        if (result.getProcessedTrips() > 0) {
            log.info("[자동환불 스케줄러] {}", result.getSummaryMessage());
        }
    }
}
