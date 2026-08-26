package com.gametrade.order.client;

import com.gametrade.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "inventory-service", path = "/api/inventory")
public interface InventoryClient {

    @PostMapping("/freeze")
    ApiResponse<Void> freeze(@RequestBody StockOp request);

    @PostMapping("/release")
    ApiResponse<Void> release(@RequestBody StockOp request);

    record StockOp(Long skuId, Integer quantity) {
    }
}
