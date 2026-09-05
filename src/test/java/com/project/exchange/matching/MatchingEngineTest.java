package com.project.exchange.matching;

import com.project.exchange.domain.Order;
import com.project.exchange.domain.OrderSide;
import com.project.exchange.domain.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class MatchingEngineTest {

    private  MatchingEngine matchingEngine;
    private OrderBook orderBook;

    @BeforeEach
    void setUp(){
        matchingEngine = new MatchingEngine();
        orderBook = new OrderBook();
    }

    private Order buildOrder(OrderSide side, BigDecimal price, long qty) {
        Instant now = Instant.now();
        return Order.builder()
                .id(UUID.randomUUID())
                .userId(1L)
                .instrument("ACME")
                .side(side)
                .limitPrice(price)
                .originalQuantity(qty)
                .remainingQuantity(qty)
                .status(OrderStatus.OPEN)
                .createdAt(now)
                .updatedAt(now)
                .sequenceNumber(1L)
                .build();
    }

    @Test
    void buyPriceBelowSellPrice_noMatch_orderRestsInBook(){
        // ARRANGE
        Order sell = buildOrder(OrderSide.SELL, new BigDecimal("100"), 10L);
        orderBook.addOrder(sell);          // put sell in book first
        Order buy = buildOrder(OrderSide.BUY, new BigDecimal("90"), 10L);
        // ACT
        MatchResult result = matchingEngine.match(buy, orderBook);
        // ASSERT
        assertThat(result.getCreatedTrades()).isEmpty();
        assertThat(result.getUpdatedIncomingOrder().getStatus()).isEqualTo(OrderStatus.OPEN);
        assertThat(orderBook.getBestSell()).isNotNull();   // sell still in book
        assertThat(orderBook.getBestBuy()).isNotNull();
    }
    @Test
    void buyAndSellSamePrice_fullMatch_bothFilled() {
        Order sell = buildOrder(OrderSide.SELL, new BigDecimal("100"), 10L);
        orderBook.addOrder(sell);
        Order buy = buildOrder(OrderSide.BUY, new BigDecimal("100"), 10L);

        MatchResult result = matchingEngine.match(buy, orderBook);

        assertThat(result.getCreatedTrades()).hasSize(1);
        assertThat(result.getUpdatedIncomingOrder().getStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(result.getUpdatedIncomingOrder().getRemainingQuantity()).isEqualTo(0L);
        assertThat(result.getCreatedTrades().get(0).getQuantity()).isEqualTo(10L);
        assertThat(result.getCreatedTrades().get(0).getExecutionPrice()).isEqualByComparingTo("100");
        assertThat(orderBook.getBestBuy()).isNull();
        assertThat(orderBook.getBestSell()).isNull();
    }

    @Test
    void buyPriceAboveSellPrice_fullMatch_executionAtSellPrice() {
        Order sell = buildOrder(OrderSide.SELL, new BigDecimal("95"), 10L);
        orderBook.addOrder(sell);
        Order buy = buildOrder(OrderSide.BUY, new BigDecimal("100"), 10L);

        MatchResult result = matchingEngine.match(buy, orderBook);

        assertThat(result.getCreatedTrades()).hasSize(1);
        assertThat(result.getCreatedTrades().get(0).getExecutionPrice()).isEqualByComparingTo("95");
        assertThat(result.getUpdatedIncomingOrder().getStatus()).isEqualTo(OrderStatus.FILLED);
    }
    @Test
    void buyQtyLessThanSellQty_buyFilledSellPartial() {
        Order sell = buildOrder(OrderSide.SELL, new BigDecimal("100"), 10L);
        orderBook.addOrder(sell);
        Order buy = buildOrder(OrderSide.BUY, new BigDecimal("100"), 4L);

        MatchResult result = matchingEngine.match(buy, orderBook);

        assertThat(result.getCreatedTrades()).hasSize(1);
        assertThat(result.getUpdatedIncomingOrder().getStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(result.getUpdatedIncomingOrder().getRemainingQuantity()).isEqualTo(0L);

        Order updatedSell = result.getUpdatedOppositeOrders().get(0);
        assertThat(updatedSell.getStatus()).isEqualTo(OrderStatus.PARTIALLY_FILLED);
        assertThat(updatedSell.getRemainingQuantity()).isEqualTo(6L);

        assertThat(orderBook.getBestSell()).isNotNull(); // sell still resting
    }
    @Test
    void buyQtyGreaterThanSellQty_sellFilledBuyPartial() {
        Order sell = buildOrder(OrderSide.SELL, new BigDecimal("100"), 4L);
        orderBook.addOrder(sell);
        Order buy = buildOrder(OrderSide.BUY, new BigDecimal("100"), 10L);

        MatchResult result = matchingEngine.match(buy, orderBook);

        assertThat(result.getCreatedTrades()).hasSize(1);
        assertThat(result.getUpdatedIncomingOrder().getStatus()).isEqualTo(OrderStatus.PARTIALLY_FILLED);
        assertThat(result.getUpdatedIncomingOrder().getRemainingQuantity()).isEqualTo(6L);

        Order updatedSell = result.getUpdatedOppositeOrders().get(0);
        assertThat(updatedSell.getStatus()).isEqualTo(OrderStatus.FILLED);

        assertThat(orderBook.getBestBuy()).isNotNull(); // buy rests with remaining qty
        assertThat(orderBook.getBestSell()).isNull();   // sell fully consumed
    }

    @Test
    void oneBuyMatchesMultipleSells_multipleTrades() {
        Order sell1 = buildOrder(OrderSide.SELL, new BigDecimal("98"), 5L);
        Order sell2 = buildOrder(OrderSide.SELL, new BigDecimal("99"), 5L);
        orderBook.addOrder(sell1);
        orderBook.addOrder(sell2);

        Order buy = buildOrder(OrderSide.BUY, new BigDecimal("100"), 10L);

        MatchResult result = matchingEngine.match(buy, orderBook);

        assertThat(result.getCreatedTrades()).hasSize(2);
        assertThat(result.getUpdatedIncomingOrder().getStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(orderBook.getBestSell()).isNull();
    }

    @Test
    void twoSellsSamePrice_earlierOneMatchedFirst() throws InterruptedException {
        Order sell1 = buildOrder(OrderSide.SELL, new BigDecimal("100"), 5L);
        Thread.sleep(10); // ensure different createdAt timestamps
        Order sell2 = buildOrder(OrderSide.SELL, new BigDecimal("100"), 5L);
        orderBook.addOrder(sell1);
        orderBook.addOrder(sell2);

        Order buy = buildOrder(OrderSide.BUY, new BigDecimal("100"), 5L);

        MatchResult result = matchingEngine.match(buy, orderBook);

        assertThat(result.getCreatedTrades()).hasSize(1);
        // sell1 should be the one matched (older)
        assertThat(result.getUpdatedOppositeOrders().get(0).getId()).isEqualTo(sell1.getId());
        assertThat(orderBook.getBestSell().getId()).isEqualTo(sell2.getId()); // sell2 still resting
    }



}
