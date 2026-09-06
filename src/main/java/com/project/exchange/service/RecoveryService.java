package com.project.exchange.service;

import com.project.exchange.domain.Order;
import com.project.exchange.domain.OrderStatus;
import com.project.exchange.matching.OrderBook;
import com.project.exchange.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecoveryService {

    private final OrderRepository orderRepository;
    private final OrderBook orderBook;

    @EventListener(ApplicationReadyEvent.class)
    public void recover(){
        log.info("Starting OrderBook recovery from database...");
        List<OrderStatus> activeStatuses =  List.of(OrderStatus.OPEN, OrderStatus.PARTIALLY_FILLED);
        List<Order> activeOrders = orderRepository.findByStatusInOrderByCreatedAtAsc(activeStatuses);
        for (Order order : activeOrders) {
            orderBook.addOrder(order);
        }
        log.info("Recovery complete! Rehydrated {} active orders into the OrderBook.", activeOrders.size());

    }
}
