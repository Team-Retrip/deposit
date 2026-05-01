package com.deposit.domain.settlement.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SettlementType {
    IMMEDIATE("즉시 정산"),
    DEFERRED("나중에 정산");

    private final String description;
}
