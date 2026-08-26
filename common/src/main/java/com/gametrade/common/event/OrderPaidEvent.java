package com.gametrade.common.event;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Emitted after a buyer successfully pays for an order.
 * Consumed by inventory (frozen -> deducted) and notification.
 */
public record OrderPaidEvent(
        Long orderId,
        Long skuId,
        Integer quantity,
        Long buyerId,
        Long sellerId,
        BigDecimal amount
) implements Serializable {
}
