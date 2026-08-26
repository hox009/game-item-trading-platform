package com.gametrade.inventory.messaging;

import com.gametrade.common.event.OrderCancelledEvent;
import com.gametrade.common.event.OrderPaidEvent;
import com.gametrade.common.event.OrderTopics;
import com.gametrade.inventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Applies order lifecycle events to authoritative stock:
 * paid -> deduct frozen; cancelled -> release frozen.
 */
@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final InventoryService inventoryService;

    public OrderEventListener(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @KafkaListener(topics = OrderTopics.ORDER_PAID, groupId = "inventory-service")
    public void onOrderPaid(OrderPaidEvent event) {
        log.info("received OrderPaidEvent order={} sku={}", event.orderId(), event.skuId());
        inventoryService.deduct(event.skuId(), event.quantity());
    }

    @KafkaListener(topics = OrderTopics.ORDER_CANCELLED, groupId = "inventory-service")
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("received OrderCancelledEvent order={} sku={}", event.orderId(), event.skuId());
        inventoryService.release(event.skuId(), event.quantity());
    }
}
