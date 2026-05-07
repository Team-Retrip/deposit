package com.deposit.application.deposit.dto.result;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class AutoRefundResult {
    private int processedTrips;
    private int refundedDeposits;
    private List<UUID> cancelledTripIds;

    public String getSummaryMessage() {
        if (processedTrips == 0) {
            return "자동 환불 대상 여행이 없습니다.";
        }
        return String.format("%d개 여행에서 %d건의 보증금이 자동 환불 처리되었습니다.",
                processedTrips, refundedDeposits);
    }
}
