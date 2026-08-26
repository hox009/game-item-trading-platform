package com.gametrade.item.dto;

import com.gametrade.item.domain.Item;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * Item view returned to clients. Serializable so it can be cached in Redis.
 */
public record ItemResponse(
        Long id,
        Long sellerId,
        String title,
        String game,
        String category,
        String description,
        String status,
        BigDecimal minPrice,
        List<SkuView> skus
) implements Serializable {

    public record SkuView(Long id, String spec, BigDecimal price) implements Serializable {
    }

    public static ItemResponse from(Item item) {
        List<SkuView> skuViews = item.getSkus().stream()
                .map(s -> new SkuView(s.getId(), s.getSpec(), s.getPrice()))
                .toList();
        return new ItemResponse(
                item.getId(),
                item.getSellerId(),
                item.getTitle(),
                item.getGame(),
                item.getCategory(),
                item.getDescription(),
                item.getStatus().name(),
                item.getMinPrice(),
                skuViews
        );
    }
}
