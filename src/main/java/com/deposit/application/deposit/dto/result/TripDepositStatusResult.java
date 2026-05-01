package com.deposit.application.deposit.dto.result;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class TripDepositStatusResult {
    private UUID tripId;
    private int totalCount;
    private int paidCount;
    private int unpaidCount;
    private boolean allPaid;
    private List<DepositResult> deposits;

    /** 미납 인원이 있으면 여행 시작 불가 메시지 포함 */
    public String getTripStartMessage() {
        if (allPaid) {
            return "모든 참가자의 보증금 납부가 완료되어 여행을 시작할 수 있습니다.";
        }
        return String.format("미납 인원 %d명이 있어 여행을 시작할 수 없습니다.", unpaidCount);
    }
}
