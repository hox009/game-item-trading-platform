package com.gametrade.inventory.dto;

import com.gametrade.inventory.domain.Stock;

public record StockResponse(Long skuId, Integer total, Integer frozen, Integer available) {

    public static StockResponse from(Stock stock) {
        return new StockResponse(stock.getSkuId(), stock.getTotal(), stock.getFrozen(), stock.available());
    }
}
