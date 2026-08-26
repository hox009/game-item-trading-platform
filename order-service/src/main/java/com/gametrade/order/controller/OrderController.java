package com.gametrade.order.controller;

import com.gametrade.common.api.ApiResponse;
import com.gametrade.common.web.GatewayHeaders;
import com.gametrade.order.dto.CreateOrderRequest;
import com.gametrade.order.dto.OrderResponse;
import com.gametrade.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ApiResponse<OrderResponse> create(@RequestHeader(GatewayHeaders.USER_ID) Long buyerId,
                                             @Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.success(orderService.createOrder(buyerId, request));
    }

    @PostMapping("/{id}/pay")
    public ApiResponse<OrderResponse> pay(@RequestHeader(GatewayHeaders.USER_ID) Long buyerId,
                                          @PathVariable Long id) {
        return ApiResponse.success(orderService.payOrder(buyerId, id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<OrderResponse> cancel(@RequestHeader(GatewayHeaders.USER_ID) Long buyerId,
                                             @PathVariable Long id) {
        return ApiResponse.success(orderService.cancelOrder(buyerId, id));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> get(@RequestHeader(GatewayHeaders.USER_ID) Long buyerId,
                                          @PathVariable Long id) {
        return ApiResponse.success(orderService.getOrder(buyerId, id));
    }

    @GetMapping
    public ApiResponse<List<OrderResponse>> list(@RequestHeader(GatewayHeaders.USER_ID) Long buyerId) {
        return ApiResponse.success(orderService.listByBuyer(buyerId));
    }
}
