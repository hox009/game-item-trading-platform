package com.gametrade.order.client;

import com.gametrade.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;

@FeignClient(name = "payment-service", path = "/api/payments")
public interface PaymentClient {

    @PostMapping("/charge")
    ApiResponse<Void> charge(@RequestBody ChargeReq request);

    record ChargeReq(Long orderId, Long buyerId, Long sellerId, BigDecimal amount) {
    }
}
