package com.gametrade.item.controller;

import com.gametrade.common.api.ApiResponse;
import com.gametrade.common.web.GatewayHeaders;
import com.gametrade.item.domain.ItemStatus;
import com.gametrade.item.dto.CreateItemRequest;
import com.gametrade.item.dto.ItemResponse;
import com.gametrade.item.dto.ItemSearchQuery;
import com.gametrade.item.service.ItemService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    /** Seller publishes an item. Seller id comes from the gateway-injected header. */
    @PostMapping
    public ApiResponse<ItemResponse> create(@RequestHeader(GatewayHeaders.USER_ID) Long sellerId,
                                            @Valid @RequestBody CreateItemRequest request) {
        return ApiResponse.success(itemService.createItem(sellerId, request));
    }

    @GetMapping("/{id}")
    public ApiResponse<ItemResponse> get(@PathVariable Long id) {
        return ApiResponse.success(itemService.getItem(id));
    }

    /** Items published by the current seller. */
    @GetMapping("/mine")
    public ApiResponse<List<ItemResponse>> mine(@RequestHeader(GatewayHeaders.USER_ID) Long sellerId) {
        return ApiResponse.success(itemService.listBySeller(sellerId));
    }

    @PostMapping("/{id}/off-shelf")
    public ApiResponse<ItemResponse> offShelf(@RequestHeader(GatewayHeaders.USER_ID) Long sellerId,
                                              @PathVariable Long id) {
        return ApiResponse.success(itemService.setStatus(sellerId, id, ItemStatus.OFF_SHELF));
    }

    @PostMapping("/{id}/on-shelf")
    public ApiResponse<ItemResponse> onShelf(@RequestHeader(GatewayHeaders.USER_ID) Long sellerId,
                                             @PathVariable Long id) {
        return ApiResponse.success(itemService.setStatus(sellerId, id, ItemStatus.ON_SHELF));
    }

    @GetMapping
    public ApiResponse<Page<ItemResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String game,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ItemSearchQuery query = new ItemSearchQuery(keyword, game, category, minPrice, maxPrice);
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("id").descending());
        return ApiResponse.success(itemService.search(query, pageable));
    }
}
