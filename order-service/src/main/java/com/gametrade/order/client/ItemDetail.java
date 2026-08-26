package com.gametrade.order.client;

import java.math.BigDecimal;
import java.util.List;

/**
 * Minimal projection of item-service's item detail that order-service needs
 * to price an order and find the seller.
 */
public record ItemDetail(
        Long id,
        Long sellerId,
        List<SkuDetail> skus
) {
    public record SkuDetail(Long id, BigDecimal price) {
    }
}
