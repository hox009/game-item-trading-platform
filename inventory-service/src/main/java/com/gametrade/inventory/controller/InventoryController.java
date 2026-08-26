package com.gametrade.inventory.controller;

import com.gametrade.common.api.ApiResponse;
import com.gametrade.inventory.dto.StockOperationRequest;
import com.gametrade.inventory.dto.StockResponse;
import com.gametrade.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal inventory API. {@code freeze} is called synchronously by
 * order-service during checkout; deduct/release also flow from Kafka events.
 */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/init")
    public ApiResponse<StockResponse> init(@RequestParam Long skuId, @RequestParam int total) {
        return ApiResponse.success(StockResponse.from(inventoryService.initStock(skuId, total)));
    }

    @PostMapping("/freeze")
    public ApiResponse<Void> freeze(@Valid @RequestBody StockOperationRequest request) {
        inventoryService.freeze(request.skuId(), request.quantity());
        return ApiResponse.success();
    }

    @PostMapping("/release")
    public ApiResponse<Void> release(@Valid @RequestBody StockOperationRequest request) {
        inventoryService.release(request.skuId(), request.quantity());
        return ApiResponse.success();
    }

    @GetMapping("/{skuId}")
    public ApiResponse<StockResponse> get(@PathVariable Long skuId) {
        return ApiResponse.success(StockResponse.from(inventoryService.getStock(skuId)));
    }
}
