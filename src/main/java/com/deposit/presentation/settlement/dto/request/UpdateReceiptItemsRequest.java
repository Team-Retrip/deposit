package com.deposit.presentation.settlement.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@NoArgsConstructor
public class UpdateReceiptItemsRequest {

    @NotEmpty
    @Valid
    private List<ItemRequest> items;

    @Getter
    @NoArgsConstructor
    public static class ItemRequest {
        @NotNull
        private Long debtorId;

        @NotNull
        @Positive
        private BigDecimal amount;
    }
}
