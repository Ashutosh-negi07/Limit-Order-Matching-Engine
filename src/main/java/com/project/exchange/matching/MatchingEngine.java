package com.project.exchange.matching;

import com.project.exchange.domain.Order;
import com.project.exchange.domain.OrderSide;
import com.project.exchange.domain.OrderStatus;
import com.project.exchange.domain.Trade;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class MatchingEngine {




     public MatchResult match(Order incomingOrder, OrderBook orderBook){

         List<Order> updatedOppositeOrders = new ArrayList<>();
         List<Trade> createdTrades = new ArrayList<>();

         while (incomingOrder.getRemainingQuantity() > 0) {

             // Step 1: find the best opposite order
             Order bestOppositeOrder;
             if (incomingOrder.getSide() == OrderSide.BUY) {
                 bestOppositeOrder = orderBook.getBestSell();
             } else {
                 bestOppositeOrder = orderBook.getBestBuy();
             }

             // Step 2: stop if no opposite order exists
             if (bestOppositeOrder == null) break;

             // Step 3: assign IDs now that null check is passed
             UUID buyOrderId  = incomingOrder.getSide() == OrderSide.BUY
                     ? incomingOrder.getId() : bestOppositeOrder.getId();
             UUID sellOrderId = incomingOrder.getSide() == OrderSide.SELL
                     ? incomingOrder.getId() : bestOppositeOrder.getId();

             // Step 4: check if prices cross
             boolean pricesCross;
             if (incomingOrder.getSide() == OrderSide.BUY) {
                 // buy price >= sell price
                 pricesCross = incomingOrder.getLimitPrice()
                         .compareTo(bestOppositeOrder.getLimitPrice()) >= 0;
             } else {
                 // sell price <= buy price
                 pricesCross = incomingOrder.getLimitPrice()
                         .compareTo(bestOppositeOrder.getLimitPrice()) <= 0;
             }
             if (!pricesCross) break;

             // Step 5: calculate matched quantity
             long matchedQty = Math.min(
                     incomingOrder.getRemainingQuantity(),
                     bestOppositeOrder.getRemainingQuantity());

             // Step 6: build trade — execution price is the resting order's price
             Trade trade = Trade.builder()
                     .executionPrice(bestOppositeOrder.getLimitPrice())
                     .quantity(matchedQty)
                     .buyOrderId(buyOrderId)
                     .sellOrderId(sellOrderId)
                     .instrument("ACME")
                     .executedAt(Instant.now())
                     .build();

             // Step 7: update quantities
             incomingOrder.setRemainingQuantity(incomingOrder.getRemainingQuantity() - matchedQty);
             bestOppositeOrder.setRemainingQuantity(bestOppositeOrder.getRemainingQuantity() - matchedQty);

             // Step 8: update opposite order status and remove if fully filled
             if (bestOppositeOrder.getRemainingQuantity() == 0) {
                 bestOppositeOrder.setStatus(OrderStatus.FILLED);
                 orderBook.removeOrder(bestOppositeOrder);
             } else {
                 bestOppositeOrder.setStatus(OrderStatus.PARTIALLY_FILLED);
             }

             createdTrades.add(trade);
             updatedOppositeOrders.add(bestOppositeOrder);
         }

         // After loop: set incoming order's final status and add to book if it has remaining quantity
         if (incomingOrder.getRemainingQuantity() == 0) {
             incomingOrder.setStatus(OrderStatus.FILLED);
         } else if (!createdTrades.isEmpty()) {
             incomingOrder.setStatus(OrderStatus.PARTIALLY_FILLED);
             orderBook.addOrder(incomingOrder);
         } else {
             incomingOrder.setStatus(OrderStatus.OPEN);
             orderBook.addOrder(incomingOrder);
         }

         

         return MatchResult.builder()
                 .updatedIncomingOrder(incomingOrder)
                 .updatedOppositeOrders(updatedOppositeOrders)
                 .createdTrades(createdTrades)
                 .build();
     }
}
