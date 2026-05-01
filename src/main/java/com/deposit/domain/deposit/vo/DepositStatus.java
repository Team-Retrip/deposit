package com.deposit.domain.deposit.vo;

public enum DepositStatus {
    PENDING,          // 납부 대기
    PAID,             // 납부 완료
    REFUND_REQUESTED, // 환불 요청
    REFUNDED          // 환불 완료
}
