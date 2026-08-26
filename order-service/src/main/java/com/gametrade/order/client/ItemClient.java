package com.gametrade.order.client;

import com.gametrade.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "item-service", path = "/api/items")
public interface ItemClient {

    @GetMapping("/{id}")
    ApiResponse<ItemDetail> getItem(@PathVariable("id") Long id);
}
