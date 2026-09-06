package com.project.exchange.repository;

import com.project.exchange.domain.Order;
import com.project.exchange.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByStatusInOrderBySequenceNumberAsc(Collection<OrderStatus> statuses);
    @Query(value = "SELECT nextval('order_sequence')", nativeQuery = true)
    Long getNextSequenceNumber();

}
