package com.gametrade.inventory.service;

import com.gametrade.common.exception.BusinessException;
import com.gametrade.inventory.domain.Stock;
import com.gametrade.inventory.lock.LockTemplate;
import com.gametrade.inventory.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InventoryServiceTest {

    private StockRepository stockRepository;
    private InventoryService inventoryService;

    /** Direct-execution lock so tests don't need Redis. */
    private final LockTemplate passthroughLock = new LockTemplate() {
        @Override
        public <T> T withLock(String key, Supplier<T> action) {
            return action.get();
        }
    };

    @BeforeEach
    void setUp() {
        stockRepository = mock(StockRepository.class);
        inventoryService = new InventoryService(stockRepository, passthroughLock);
        when(stockRepository.save(any(Stock.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Stock stock(long skuId, int total, int frozen) {
        Stock s = new Stock();
        s.setSkuId(skuId);
        s.setTotal(total);
        s.setFrozen(frozen);
        return s;
    }

    @Test
    void freeze_succeedsWhenEnoughAvailable() {
        Stock s = stock(1L, 10, 2);
        when(stockRepository.findById(1L)).thenReturn(Optional.of(s));

        inventoryService.freeze(1L, 5);

        assertThat(s.getFrozen()).isEqualTo(7);
        assertThat(s.available()).isEqualTo(3);
    }

    @Test
    void freeze_rejectsWhenNotEnough() {
        Stock s = stock(1L, 10, 8);
        when(stockRepository.findById(1L)).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> inventoryService.freeze(1L, 5))
                .isInstanceOf(BusinessException.class);
        assertThat(s.getFrozen()).isEqualTo(8);
    }

    @Test
    void deduct_reducesBothTotalAndFrozen() {
        Stock s = stock(1L, 10, 5);
        when(stockRepository.findById(1L)).thenReturn(Optional.of(s));

        inventoryService.deduct(1L, 5);

        assertThat(s.getTotal()).isEqualTo(5);
        assertThat(s.getFrozen()).isEqualTo(0);
    }

    @Test
    void release_reducesFrozenOnly() {
        Stock s = stock(1L, 10, 5);
        when(stockRepository.findById(1L)).thenReturn(Optional.of(s));

        inventoryService.release(1L, 3);

        assertThat(s.getTotal()).isEqualTo(10);
        assertThat(s.getFrozen()).isEqualTo(2);
    }
}
