package com.deposit.common.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class UserContext {
    private final UUID memberId;
}
