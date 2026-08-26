package com.gametrade.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Internal escrow charge: move {@code amount} from buyer to seller for an order.
 */
public record ChargeRequest(
        @NotNull Long orderId,
        @NotNull Long buyerId,
        @NotNull Long sellerId,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount
) {
}
