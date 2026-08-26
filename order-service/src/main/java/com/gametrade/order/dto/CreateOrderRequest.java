package com.gametrade.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
        @NotNull Long itemId,
        @NotNull Long skuId,
        @NotNull @Min(1) Integer quantity
) {
}
