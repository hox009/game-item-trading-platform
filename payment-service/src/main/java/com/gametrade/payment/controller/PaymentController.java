package com.gametrade.payment.controller;

import com.gametrade.common.api.ApiResponse;
import com.gametrade.payment.dto.ChargeRequest;
import com.gametrade.payment.dto.RechargeRequest;
import com.gametrade.payment.dto.WalletResponse;
import com.gametrade.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/recharge")
    public ApiResponse<WalletResponse> recharge(@Valid @RequestBody RechargeRequest request) {
        return ApiResponse.success(WalletResponse.from(paymentService.recharge(request.userId(), request.amount())));
    }

    /** Internal endpoint invoked by order-service during payment. */
    @PostMapping("/charge")
    public ApiResponse<Void> charge(@Valid @RequestBody ChargeRequest request) {
        paymentService.charge(request);
        return ApiResponse.success();
    }

    @GetMapping("/wallet/{userId}")
    public ApiResponse<WalletResponse> wallet(@PathVariable Long userId) {
        return ApiResponse.success(WalletResponse.from(paymentService.getWallet(userId)));
    }
}
