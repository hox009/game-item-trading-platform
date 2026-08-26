package com.gametrade.inventory.service;

import com.gametrade.common.api.ResultCode;
import com.gametrade.common.exception.BusinessException;
import com.gametrade.inventory.domain.Stock;
import com.gametrade.inventory.lock.LockTemplate;
import com.gametrade.inventory.repository.StockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
    private static final String LOCK_PREFIX = "lock:stock:";

    private final StockRepository stockRepository;
    private final LockTemplate lockTemplate;

    public InventoryService(StockRepository stockRepository, LockTemplate lockTemplate) {
        this.stockRepository = stockRepository;
        this.lockTemplate = lockTemplate;
    }

    /** Seed or reset the stock total for a SKU. */
    @Transactional
    public Stock initStock(Long skuId, int total) {
        Stock stock = stockRepository.findById(skuId).orElseGet(() -> {
            Stock s = new Stock();
            s.setSkuId(skuId);
            return s;
        });
        stock.setTotal(total);
        if (stock.getFrozen() > total) {
            stock.setFrozen(total);
        }
        return stockRepository.save(stock);
    }

    /**
     * Reserves stock for an order. Guarded by a per-SKU distributed lock so
     * concurrent buyers cannot oversell.
     */
    @Transactional
    public void freeze(Long skuId, int quantity) {
        lockTemplate.withLock(LOCK_PREFIX + skuId, () -> {
            Stock stock = requireStock(skuId);
            if (stock.available() < quantity) {
                throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH);
            }
            stock.setFrozen(stock.getFrozen() + quantity);
            stockRepository.save(stock);
            return null;
        });
    }

    /** Releases previously frozen stock (order cancelled/timed out). Idempotent-ish. */
    @Transactional
    public void release(Long skuId, int quantity) {
        lockTemplate.withLock(LOCK_PREFIX + skuId, () -> {
            Stock stock = requireStock(skuId);
            int newFrozen = Math.max(0, stock.getFrozen() - quantity);
            stock.setFrozen(newFrozen);
            stockRepository.save(stock);
            return null;
        });
    }

    /** Converts frozen stock into a real deduction after successful payment. */
    @Transactional
    public void deduct(Long skuId, int quantity) {
        lockTemplate.withLock(LOCK_PREFIX + skuId, () -> {
            Stock stock = requireStock(skuId);
            int newFrozen = Math.max(0, stock.getFrozen() - quantity);
            int newTotal = Math.max(0, stock.getTotal() - quantity);
            stock.setFrozen(newFrozen);
            stock.setTotal(newTotal);
            stockRepository.save(stock);
            log.info("deducted sku={} qty={} remainingTotal={}", skuId, quantity, newTotal);
            return null;
        });
    }

    @Transactional(readOnly = true)
    public Stock getStock(Long skuId) {
        return requireStock(skuId);
    }

    private Stock requireStock(Long skuId) {
        return stockRepository.findById(skuId)
                .orElseThrow(() -> new BusinessException(ResultCode.ITEM_NOT_FOUND, "stock not found for sku " + skuId));
    }
}
