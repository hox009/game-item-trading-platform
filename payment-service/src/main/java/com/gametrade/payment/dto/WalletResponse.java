package com.gametrade.payment.dto;

import com.gametrade.payment.domain.Wallet;

import java.math.BigDecimal;

public record WalletResponse(Long userId, BigDecimal balance) {

    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(wallet.getUserId(), wallet.getBalance());
    }
}
