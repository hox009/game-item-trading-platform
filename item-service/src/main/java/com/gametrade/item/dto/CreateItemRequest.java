package com.gametrade.item.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * Payload for a seller publishing a new item with one or more SKU variants.
 */
public record CreateItemRequest(
        @NotBlank String title,
        @NotBlank String game,
        @NotBlank String category,
        String description,
        @NotEmpty @Valid List<SkuInput> skus
) {
    public record SkuInput(
            @NotBlank String spec,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price
    ) {
    }
}
