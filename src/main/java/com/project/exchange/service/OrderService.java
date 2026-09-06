package com.project.exchange.service;



import com.project.exchange.domain.Order;
import com.project.exchange.domain.OrderStatus;
import com.project.exchange.domain.Trade;
import com.project.exchange.matching.MatchResult;
import com.project.exchange.matching.MatchingEngine;
import com.project.exchange.matching.OrderBook;
import com.project.exchange.repository.OrderRepository;
import com.project.exchange.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;
    private final MatchingEngine matchingEngine;
    private final OrderBook orderBook;
    private final ReentrantLock lock = new ReentrantLock();

    public void processOrder(Order incomingOrder){
        lock.lock();
        try {
            orderRepository.save(incomingOrder);
            MatchResult matchResult = matchingEngine.match(incomingOrder, orderBook);
            orderRepository.save(matchResult.getUpdatedIncomingOrder());
            for (Order order : matchResult.getUpdatedOppositeOrders()) {
                orderRepository.save(order);
            }
            for (Trade trade : matchResult.getCreatedTrades()) {
                tradeRepository.save(trade);
            }
        }finally{
            lock.unlock();
        }

    }

    public Order cancelOrder(UUID id){
        lock.lock();
        try{
            Order order = orderRepository.findById(id)
                    .orElseThrow(()->new NoSuchElementException("Order not found with id: " + id));

            if (order.getStatus() == OrderStatus.FILLED || order.getStatus() == OrderStatus.CANCELLED) {
                throw new IllegalStateException("Cannot cancel order with status: " + order.getStatus());
            }
            orderBook.removeOrder(order);

            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            return order;
        }
        finally{lock.unlock();}
    }


    public Order getOrderById(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found with id: " + id));
    }

    public List<Trade> getAllTrades() {
        return tradeRepository.findAll();
    }

    public OrderBook getOrderBook() {
        return orderBook;
    }

}
