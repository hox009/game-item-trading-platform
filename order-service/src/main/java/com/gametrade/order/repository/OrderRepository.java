package com.gametrade.order.repository;

import com.gametrade.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByBuyerIdOrderByIdDesc(Long buyerId);
}
