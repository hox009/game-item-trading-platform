package com.gametrade.order.domain;

/**
 * Order lifecycle.
 * <pre>
 *   CREATED -> STOCK_FROZEN -> PAID -> COMPLETED
 *        \-------> CANCELLED (release stock)
 * </pre>
 */
public enum OrderStatus {
    CREATED,
    STOCK_FROZEN,
    PAID,
    COMPLETED,
    CANCELLED
}
