package com.gametrade.order.dto;

import com.gametrade.order.domain.Order;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
        Long id,
        Long buyerId,
        Long sellerId,
        Long itemId,
        Long skuId,
        Integer quantity,
        BigDecimal amount,
        String status,
        Instant createdAt,
        Instant paidAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getBuyerId(),
                order.getSellerId(),
                order.getItemId(),
                order.getSkuId(),
                order.getQuantity(),
                order.getAmount(),
                order.getStatus().name(),
                order.getCreatedAt(),
                order.getPaidAt()
        );
    }
}
