package com.deposit.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Deposit Policy
    DEPOSIT_POLICY_NOT_FOUND(HttpStatus.NOT_FOUND, "보증금 정책을 찾을 수 없습니다."),
    DEPOSIT_POLICY_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 보증금 정책이 설정된 여행입니다."),
    DEPOSIT_POLICY_NOT_SAFE_TRIP(HttpStatus.BAD_REQUEST, "안전한 여행이 아닌 경우 보증금을 요청할 수 없습니다."),
    TRIP_ALREADY_CONFIRMED(HttpStatus.CONFLICT, "이미 확정된 여행입니다."),

    // Deposit
    DEPOSIT_NOT_FOUND(HttpStatus.NOT_FOUND, "보증금 내역을 찾을 수 없습니다."),
    DEPOSIT_ALREADY_EXISTS(HttpStatus.CONFLICT, "해당 사용자의 보증금 요청이 이미 존재합니다."),
    DEPOSIT_ALREADY_PAID(HttpStatus.CONFLICT, "이미 납부된 보증금입니다."),
    DEPOSIT_NOT_PAID(HttpStatus.BAD_REQUEST, "아직 보증금이 납부되지 않았습니다."),
    DEPOSIT_ALREADY_REFUNDED(HttpStatus.CONFLICT, "이미 환불된 보증금입니다."),
    DEPOSIT_REFUND_IN_PROGRESS(HttpStatus.CONFLICT, "환불 처리 중인 보증금입니다."),
    DEPOSIT_UNPAID_EXISTS(HttpStatus.BAD_REQUEST, "미납된 보증금이 있어 여행을 시작할 수 없습니다."),

    // Settlement
    SETTLEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "정산 내역을 찾을 수 없습니다."),
    SETTLEMENT_BALANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "정산 누적 잔액을 찾을 수 없습니다."),
    SETTLEMENT_ALREADY_CANCELLED(HttpStatus.CONFLICT, "이미 취소된 정산 내역입니다."),
    SETTLEMENT_ALREADY_COMPLETED(HttpStatus.CONFLICT, "이미 완료된 정산 내역입니다."),
    SETTLEMENT_NOT_COMPLETABLE(HttpStatus.BAD_REQUEST, "완료 처리할 수 없는 상태의 정산입니다."),

    SETTLEMENT_SPLIT_INVALID_DEBTORS(HttpStatus.BAD_REQUEST, "정산 대상 인원은 1명 이상이어야 합니다."),
    SETTLEMENT_CUSTOM_AMOUNTS_MISMATCH(HttpStatus.BAD_REQUEST, "개별 정산 금액의 합계가 총 금액과 일치하지 않습니다."),

    // Settlement Receipt
    RECEIPT_NOT_FOUND(HttpStatus.NOT_FOUND, "정산 영수증을 찾을 수 없습니다."),
    RECEIPT_ALREADY_CANCELLED(HttpStatus.CONFLICT, "이미 취소된 정산 영수증입니다."),
    RECEIPT_ITEMS_TOTAL_MISMATCH(HttpStatus.BAD_REQUEST, "개별 금액의 합계가 총액과 일치하지 않습니다."),
    RECEIPT_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 채무자의 정산 항목을 찾을 수 없습니다."),

    // PG
    PG_PAYMENT_FAILED(HttpStatus.BAD_GATEWAY, "결제 처리 중 오류가 발생했습니다."),
    PG_REFUND_FAILED(HttpStatus.BAD_GATEWAY, "환불 처리 중 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
