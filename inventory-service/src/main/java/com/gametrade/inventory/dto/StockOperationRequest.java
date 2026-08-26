package com.gametrade.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockOperationRequest(
        @NotNull Long skuId,
        @NotNull @Min(1) Integer quantity
) {
}
