package com.gametrade.payment.service;

import com.gametrade.common.api.ResultCode;
import com.gametrade.common.exception.BusinessException;
import com.gametrade.payment.domain.LedgerEntry;
import com.gametrade.payment.domain.LedgerType;
import com.gametrade.payment.domain.Wallet;
import com.gametrade.payment.dto.ChargeRequest;
import com.gametrade.payment.repository.LedgerRepository;
import com.gametrade.payment.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class PaymentService {

    private final WalletRepository walletRepository;
    private final LedgerRepository ledgerRepository;

    public PaymentService(WalletRepository walletRepository, LedgerRepository ledgerRepository) {
        this.walletRepository = walletRepository;
        this.ledgerRepository = ledgerRepository;
    }

    /** Mock top-up so buyers have balance to trade with. */
    @Transactional
    public Wallet recharge(Long userId, BigDecimal amount) {
        Wallet wallet = getOrCreateWallet(userId);
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);
        writeLedger(userId, LedgerType.RECHARGE, amount, wallet.getBalance(), null);
        return wallet;
    }

    /**
     * Escrow charge: debit the buyer and credit the seller atomically.
     * Runs in a single transaction so a failure leaves no half-applied balances.
     */
    @Transactional
    public void charge(ChargeRequest request) {
        Wallet buyer = getOrCreateWallet(request.buyerId());
        if (buyer.getBalance().compareTo(request.amount()) < 0) {
            throw new BusinessException(ResultCode.BALANCE_NOT_ENOUGH);
        }
        buyer.setBalance(buyer.getBalance().subtract(request.amount()));
        walletRepository.save(buyer);
        writeLedger(request.buyerId(), LedgerType.DEBIT, request.amount(), buyer.getBalance(), request.orderId());

        Wallet seller = getOrCreateWallet(request.sellerId());
        seller.setBalance(seller.getBalance().add(request.amount()));
        walletRepository.save(seller);
        writeLedger(request.sellerId(), LedgerType.CREDIT, request.amount(), seller.getBalance(), request.orderId());
    }

    @Transactional(readOnly = true)
    public Wallet getWallet(Long userId) {
        return walletRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "wallet not found"));
    }

    private Wallet getOrCreateWallet(Long userId) {
        return walletRepository.findById(userId).orElseGet(() -> {
            Wallet w = new Wallet();
            w.setUserId(userId);
            w.setBalance(BigDecimal.ZERO);
            return walletRepository.save(w);
        });
    }

    private void writeLedger(Long userId, LedgerType type, BigDecimal amount, BigDecimal balanceAfter, Long orderId) {
        LedgerEntry entry = new LedgerEntry();
        entry.setUserId(userId);
        entry.setType(type);
        entry.setAmount(amount);
        entry.setBalanceAfter(balanceAfter);
        entry.setRefOrderId(orderId);
        ledgerRepository.save(entry);
    }
}
