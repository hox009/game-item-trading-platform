package com.gametrade.item.dto;

import java.math.BigDecimal;

/**
 * Optional search/filter criteria for browsing the catalog.
 */
public record ItemSearchQuery(
        String keyword,
        String game,
        String category,
        BigDecimal minPrice,
        BigDecimal maxPrice
) {
}
