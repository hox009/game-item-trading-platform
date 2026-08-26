package com.gametrade.common.event;

import java.io.Serializable;

/**
 * Emitted when an order is cancelled or times out before payment.
 * Consumed by inventory to release the previously frozen stock.
 */
public record OrderCancelledEvent(
        Long orderId,
        Long skuId,
        Integer quantity,
        Long buyerId
) implements Serializable {
}
