package com.deposit.domain.settlement.vo;

public enum SettlementStatus {
    PENDING,      // 생성됨 (처리 전 과도 상태)
    NOTIFIED,     // 즉시 정산: 알림 발송 완료
    ACCUMULATED,  // 나중에 정산: 누적됨
    COMPLETED,    // 정산 완료 처리됨
    CANCELLED     // 취소됨
}
