package com.gametrade.payment.service;

import com.gametrade.common.exception.BusinessException;
import com.gametrade.payment.domain.LedgerEntry;
import com.gametrade.payment.domain.Wallet;
import com.gametrade.payment.dto.ChargeRequest;
import com.gametrade.payment.repository.LedgerRepository;
import com.gametrade.payment.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

    private WalletRepository walletRepository;
    private LedgerRepository ledgerRepository;
    private PaymentService paymentService;
    private Map<Long, Wallet> store;

    @BeforeEach
    void setUp() {
        walletRepository = mock(WalletRepository.class);
        ledgerRepository = mock(LedgerRepository.class);
        paymentService = new PaymentService(walletRepository, ledgerRepository);
        store = new HashMap<>();

        when(walletRepository.findById(any())).thenAnswer(inv -> Optional.ofNullable(store.get(inv.getArgument(0))));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> {
            Wallet w = inv.getArgument(0);
            store.put(w.getUserId(), w);
            return w;
        });
        when(ledgerRepository.save(any(LedgerEntry.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void charge_movesFundsFromBuyerToSeller() {
        paymentService.recharge(1L, new BigDecimal("100.00"));

        paymentService.charge(new ChargeRequest(9000L, 1L, 2L, new BigDecimal("30.00")));

        assertThat(store.get(1L).getBalance()).isEqualByComparingTo("70.00");
        assertThat(store.get(2L).getBalance()).isEqualByComparingTo("30.00");
    }

    @Test
    void charge_rejectsWhenBalanceInsufficient() {
        paymentService.recharge(1L, new BigDecimal("10.00"));

        assertThatThrownBy(() -> paymentService.charge(new ChargeRequest(9000L, 1L, 2L, new BigDecimal("30.00"))))
                .isInstanceOf(BusinessException.class);
        assertThat(store.get(1L).getBalance()).isEqualByComparingTo("10.00");
    }
}
