package com.deposit.presentation.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ConfirmTripRequest {

    @NotNull(message = "방장 ID를 입력해주세요.")
    private Long organizerId;
}
