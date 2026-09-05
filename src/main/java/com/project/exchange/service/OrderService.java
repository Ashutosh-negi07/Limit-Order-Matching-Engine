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

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;
    private final MatchingEngine matchingEngine;
    private final OrderBook orderBook;

    public void processOrder(Order incomingOrder){

        orderRepository.save(incomingOrder);
        MatchResult matchResult = matchingEngine.match(incomingOrder, orderBook);
        orderRepository.save(matchResult.getUpdatedIncomingOrder());
        for(Order order:matchResult.getUpdatedOppositeOrders()){
            orderRepository.save(order);
        }
        for(Trade trade:matchResult.getCreatedTrades()){
            tradeRepository.save(trade);
        }

    }
}
