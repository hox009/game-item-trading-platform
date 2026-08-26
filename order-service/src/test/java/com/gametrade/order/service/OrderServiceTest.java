package com.gametrade.order.service;

import com.gametrade.common.api.ApiResponse;
import com.gametrade.common.event.OrderPaidEvent;
import com.gametrade.common.exception.BusinessException;
import com.gametrade.order.client.InventoryClient;
import com.gametrade.order.client.ItemClient;
import com.gametrade.order.client.ItemDetail;
import com.gametrade.order.client.PaymentClient;
import com.gametrade.order.domain.Order;
import com.gametrade.order.domain.OrderStatus;
import com.gametrade.order.dto.CreateOrderRequest;
import com.gametrade.order.dto.OrderResponse;
import com.gametrade.order.messaging.NotificationPublisher;
import com.gametrade.order.messaging.OrderEventPublisher;
import com.gametrade.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    private OrderRepository orderRepository;
    private ItemClient itemClient;
    private InventoryClient inventoryClient;
    private PaymentClient paymentClient;
    private OrderEventPublisher eventPublisher;
    private NotificationPublisher notificationPublisher;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        itemClient = mock(ItemClient.class);
        inventoryClient = mock(InventoryClient.class);
        paymentClient = mock(PaymentClient.class);
        eventPublisher = mock(OrderEventPublisher.class);
        notificationPublisher = mock(NotificationPublisher.class);
        orderService = new OrderService(orderRepository, itemClient, inventoryClient, paymentClient, eventPublisher, notificationPublisher);

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            if (o.getId() == null) {
                o.setId(1L);
            }
            return o;
        });
    }

    private void stubItem() {
        ItemDetail detail = new ItemDetail(50L, 2L,
                List.of(new ItemDetail.SkuDetail(500L, new BigDecimal("20.00"))));
        when(itemClient.getItem(50L)).thenReturn(ApiResponse.success(detail));
    }

    @Test
    void createOrder_freezesStockAndReturnsFrozenOrder() {
        stubItem();
        when(inventoryClient.freeze(any())).thenReturn(ApiResponse.success());

        OrderResponse response = orderService.createOrder(1L, new CreateOrderRequest(50L, 500L, 3));

        assertThat(response.status()).isEqualTo("STOCK_FROZEN");
        assertThat(response.amount()).isEqualByComparingTo("60.00");
        assertThat(response.sellerId()).isEqualTo(2L);
    }

    @Test
    void createOrder_marksCancelledWhenFreezeFails() {
        stubItem();
        when(inventoryClient.freeze(any()))
                .thenReturn(ApiResponse.failure(com.gametrade.common.api.ResultCode.STOCK_NOT_ENOUGH));

        assertThatThrownBy(() -> orderService.createOrder(1L, new CreateOrderRequest(50L, 500L, 3)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void payOrder_chargesAndPublishesPaidEvent() {
        Order order = new Order();
        order.setId(1L);
        order.setBuyerId(1L);
        order.setSellerId(2L);
        order.setSkuId(500L);
        order.setQuantity(3);
        order.setAmount(new BigDecimal("60.00"));
        order.setStatus(OrderStatus.STOCK_FROZEN);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentClient.charge(any())).thenReturn(ApiResponse.success());

        OrderResponse response = orderService.payOrder(1L, 1L);

        assertThat(response.status()).isEqualTo("PAID");
        verify(eventPublisher).publishPaid(any(OrderPaidEvent.class));
    }

    @Test
    void payOrder_rejectsWhenNotFrozen() {
        Order order = new Order();
        order.setId(1L);
        order.setBuyerId(1L);
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.payOrder(1L, 1L))
                .isInstanceOf(BusinessException.class);
        verify(paymentClient, never()).charge(any());
    }
}
