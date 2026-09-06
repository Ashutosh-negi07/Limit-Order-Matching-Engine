package com.project.exchange.service;

import com.project.exchange.domain.Order;
import com.project.exchange.domain.OrderSide;
import com.project.exchange.domain.OrderStatus;
import com.project.exchange.matching.MatchingEngine;
import com.project.exchange.matching.OrderBook;
import com.project.exchange.repository.OrderRepository;
import com.project.exchange.repository.TradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class OrderServiceConcurrencyTest {

    private OrderRepository orderRepository;
    private TradeRepository tradeRepository;
    private OrderBook orderBook;
    private MatchingEngine matchingEngine;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = Mockito.mock(OrderRepository.class);
        tradeRepository = Mockito.mock(TradeRepository.class);
        orderBook = new OrderBook();
        matchingEngine = new MatchingEngine();

        orderService = new OrderService(orderRepository, tradeRepository, matchingEngine, orderBook);
    }

    @RepeatedTest(10) // Run 10 times to catch subtle race conditions
    void testCancelVsMatchRaceCondition() throws InterruptedException {
        // 1. Create a resting BUY order in the book
        UUID orderId = UUID.randomUUID();
        Order restingBuy = Order.builder()
                .id(orderId)
                .userId(1L)
                .instrument("ACME")
                .side(OrderSide.BUY)
                .limitPrice(new BigDecimal("50.00"))
                .originalQuantity(100L)
                .remainingQuantity(100L)
                .status(OrderStatus.OPEN)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .sequenceNumber(1L)
                .build();

        orderBook.addOrder(restingBuy);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(restingBuy));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 2. Setup threads with a starting pistol (CountDownLatch)
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch finishGate = new CountDownLatch(2);

        // Thread 1: Try to Cancel
        executor.submit(() -> {
            try {
                startGun.await(); // wait for the gun
                orderService.cancelOrder(orderId);
            } catch (Exception ignored) {
                // If match won first, cancel throws IllegalStateException (409) — which is expected!
            } finally {
                finishGate.countDown();
            }
        });

        // Thread 2: Try to Match with a SELL order
        executor.submit(() -> {
            try {
                startGun.await(); // wait for the gun
                Order incomingSell = Order.builder()
                        .id(UUID.randomUUID())
                        .userId(2L)
                        .instrument("ACME")
                        .side(OrderSide.SELL)
                        .limitPrice(new BigDecimal("50.00"))
                        .originalQuantity(100L)
                        .remainingQuantity(100L)
                        .status(OrderStatus.OPEN)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .sequenceNumber(2L)
                        .build();

                orderService.processOrder(incomingSell);
            } catch (Exception ignored) {
            } finally {
                finishGate.countDown();
            }
        });

        // 3. Fire the pistol!
        startGun.countDown();

        // 4. Wait for both threads to finish (max 2 seconds)
        assertTrue(finishGate.await(2, TimeUnit.SECONDS));
        executor.shutdown();

        // 5. CRITICAL INVARIANTS: Check state integrity
        OrderStatus finalStatus = restingBuy.getStatus();

        // Assert: Must be either FILLED or CANCELLED, but NEVER in an intermediate invalid state
        assertTrue(finalStatus == OrderStatus.FILLED || finalStatus == OrderStatus.CANCELLED,
                "Order should either be FILLED or CANCELLED, but was: " + finalStatus);

        // Assert: Remaining quantity must never be negative
        assertTrue(restingBuy.getRemainingQuantity() >= 0,
                "Remaining quantity must never be negative: " + restingBuy.getRemainingQuantity());
    }
    @RepeatedTest(5)
    void testMultipleConcurrentCrossingOrders() throws InterruptedException {
        int orderCount = 20; // 10 buys and 10 sells
        ExecutorService executor = Executors.newFixedThreadPool(orderCount);
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch finishGate = new CountDownLatch(orderCount);

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Submit 10 BUY orders of 10 shares @ $50.00
        for (int i = 0; i < 10; i++) {
            final long userId = i + 1;
            executor.submit(() -> {
                try {
                    startGun.await();
                    Order buy = Order.builder()
                            .id(UUID.randomUUID())
                            .userId(userId)
                            .instrument("ACME")
                            .side(OrderSide.BUY)
                            .limitPrice(new BigDecimal("50.00"))
                            .originalQuantity(10L)
                            .remainingQuantity(10L)
                            .status(OrderStatus.OPEN)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .sequenceNumber(userId)
                            .build();
                    orderService.processOrder(buy);
                } catch (Exception ignored) {
                } finally {
                    finishGate.countDown();
                }
            });
        }

        // Submit 10 SELL orders of 10 shares @ $50.00
        for (int i = 0; i < 10; i++) {
            final long userId = i + 11;
            executor.submit(() -> {
                try {
                    startGun.await();
                    Order sell = Order.builder()
                            .id(UUID.randomUUID())
                            .userId(userId)
                            .instrument("ACME")
                            .side(OrderSide.SELL)
                            .limitPrice(new BigDecimal("50.00"))
                            .originalQuantity(10L)
                            .remainingQuantity(10L)
                            .status(OrderStatus.OPEN)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .sequenceNumber(userId)
                            .build();
                    orderService.processOrder(sell);
                } catch (Exception ignored) {
                } finally {
                    finishGate.countDown();
                }
            });
        }

        // Release all 20 threads simultaneously
        startGun.countDown();

        assertTrue(finishGate.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        // INVARIANT CHECK: All 10 buys and 10 sells cross perfectly.
        // The order book should be completely empty (all orders FILLED).
        assertEquals(0, orderBook.getBuyOrders().size(), "All buy orders should have matched and cleared from the book");
        assertEquals(0, orderBook.getSellOrders().size(), "All sell orders should have matched and cleared from the book");
    }

}
