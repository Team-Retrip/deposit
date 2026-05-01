package com.deposit.presentation.api.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.List;

@Getter
public class RequestDepositRequest {

    @NotNull(message = "방장 ID를 입력해주세요.")
    private Long organizerId;

    @NotEmpty(message = "보증금을 요청할 참가자 ID 목록을 입력해주세요.")
    private List<Long> userIds;
}
