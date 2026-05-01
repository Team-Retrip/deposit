package com.deposit.domain.deposit.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentMethod {
    CARD("카드 결제"),
    BANK_TRANSFER("계좌이체");

    private final String description;
}
