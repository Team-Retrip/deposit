package com.deposit.domain.deposit.vo;

public enum PolicyStatus {
    ACTIVE,     // 납부 대기 중
    CONFIRMED,  // 여행 확정 (전원 납부 완료)
    CANCELLED,  // 자동 취소 (여행 시작 시간 초과, 자동 환불)
    INACTIVE    // 비활성
}
