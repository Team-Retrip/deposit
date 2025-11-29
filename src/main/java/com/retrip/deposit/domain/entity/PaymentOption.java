package com.retrip.deposit.domain.entity;

import java.util.Arrays;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentOption {
    CARD("CARD");
    private final String code;
    public static PaymentOption codeOf(String code) {
        return Arrays.stream(PaymentOption.values())
                .filter(paymentOption -> paymentOption.getCode().equals(code))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 코드입니다."));
    }

}
