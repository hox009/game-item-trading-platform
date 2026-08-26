package com.gametrade.order.service;

import com.gametrade.common.api.ApiResponse;
import com.gametrade.common.api.ResultCode;
import com.gametrade.common.event.OrderCancelledEvent;
import com.gametrade.common.event.OrderPaidEvent;
import com.gametrade.common.exception.BusinessException;
import com.gametrade.order.client.InventoryClient;
import com.gametrade.order.client.ItemClient;
import com.gametrade.order.client.ItemDetail;
import com.gametrade.order.client.PaymentClient;
import com.gametrade.order.domain.Order;
import com.gametrade.order.domain.OrderStatus;
import com.gametrade.common.notify.NotificationMessage;
import com.gametrade.order.dto.CreateOrderRequest;
import com.gametrade.order.dto.OrderResponse;
import com.gametrade.order.messaging.NotificationPublisher;
import com.gametrade.order.messaging.OrderEventPublisher;
import com.gametrade.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ItemClient itemClient;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    private final OrderEventPublisher eventPublisher;
    private final NotificationPublisher notificationPublisher;

    public OrderService(OrderRepository orderRepository,
                        ItemClient itemClient,
                        InventoryClient inventoryClient,
                        PaymentClient paymentClient,
                        OrderEventPublisher eventPublisher,
                        NotificationPublisher notificationPublisher) {
        this.orderRepository = orderRepository;
        this.itemClient = itemClient;
        this.inventoryClient = inventoryClient;
        this.paymentClient = paymentClient;
        this.eventPublisher = eventPublisher;
        this.notificationPublisher = notificationPublisher;
    }

    /**
     * Creates an order and synchronously freezes stock. If freezing fails the
     * order is marked cancelled so no dangling reservation remains.
     */
    @Transactional
    public OrderResponse createOrder(Long buyerId, CreateOrderRequest request) {
        ItemDetail item = unwrap(itemClient.getItem(request.itemId()));
        if (item == null) {
            throw new BusinessException(ResultCode.ITEM_NOT_FOUND);
        }
        BigDecimal unitPrice = item.skus().stream()
                .filter(s -> s.id().equals(request.skuId()))
                .map(ItemDetail.SkuDetail::price)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.ITEM_NOT_FOUND, "sku not found"));

        Order order = new Order();
        order.setBuyerId(buyerId);
        order.setSellerId(item.sellerId());
        order.setItemId(item.id());
        order.setSkuId(request.skuId());
        order.setQuantity(request.quantity());
        order.setAmount(unitPrice.multiply(BigDecimal.valueOf(request.quantity())));
        order.setStatus(OrderStatus.CREATED);
        order = orderRepository.save(order);

        try {
            unwrap(inventoryClient.freeze(new InventoryClient.StockOp(request.skuId(), request.quantity())));
        } catch (BusinessException ex) {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            throw ex;
        }

        order.setStatus(OrderStatus.STOCK_FROZEN);
        order = orderRepository.save(order);
        log.info("order {} created and stock frozen", order.getId());
        return OrderResponse.from(order);
    }

    /**
     * Charges the buyer, then emits {@link OrderPaidEvent} so inventory finalizes
     * the deduction asynchronously (event-driven order processing).
     */
    @Transactional
    public OrderResponse payOrder(Long buyerId, Long orderId) {
        Order order = requireOwnedOrder(buyerId, orderId);
        if (order.getStatus() != OrderStatus.STOCK_FROZEN) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ILLEGAL);
        }

        unwrap(paymentClient.charge(new PaymentClient.ChargeReq(
                order.getId(), order.getBuyerId(), order.getSellerId(), order.getAmount())));

        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(Instant.now());
        order = orderRepository.save(order);

        eventPublisher.publishPaid(new OrderPaidEvent(
                order.getId(), order.getSkuId(), order.getQuantity(),
                order.getBuyerId(), order.getSellerId(), order.getAmount()));
        notificationPublisher.send(new NotificationMessage(
                order.getBuyerId(), "Payment successful",
                "Your order #" + order.getId() + " has been paid.", order.getId()));
        log.info("order {} paid", order.getId());
        return OrderResponse.from(order);
    }

    /** Cancels an unpaid order and releases frozen stock via an event. */
    @Transactional
    public OrderResponse cancelOrder(Long buyerId, Long orderId) {
        Order order = requireOwnedOrder(buyerId, orderId);
        if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.STOCK_FROZEN) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ILLEGAL);
        }
        boolean wasFrozen = order.getStatus() == OrderStatus.STOCK_FROZEN;
        order.setStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);

        if (wasFrozen) {
            eventPublisher.publishCancelled(new OrderCancelledEvent(
                    order.getId(), order.getSkuId(), order.getQuantity(), order.getBuyerId()));
        }
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long buyerId, Long orderId) {
        return OrderResponse.from(requireOwnedOrder(buyerId, orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listByBuyer(Long buyerId) {
        return orderRepository.findByBuyerIdOrderByIdDesc(buyerId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    private Order requireOwnedOrder(Long buyerId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ResultCode.ORDER_NOT_FOUND));
        if (!order.getBuyerId().equals(buyerId)) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    /** Unwraps a downstream {@link ApiResponse}, converting error codes into exceptions. */
    private <T> T unwrap(ApiResponse<T> response) {
        if (response == null) {
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE);
        }
        if (response.getCode() != ResultCode.SUCCESS.getCode()) {
            throw new BusinessException(response.getCode(), response.getMessage());
        }
        return response.getData();
    }
}
