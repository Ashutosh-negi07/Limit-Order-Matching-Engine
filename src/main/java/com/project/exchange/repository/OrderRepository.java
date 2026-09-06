package com.project.exchange.repository;

import com.project.exchange.domain.Order;
import com.project.exchange.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByStatusInOrderByCreatedAtAsc(Collection<OrderStatus> statuses);

}
