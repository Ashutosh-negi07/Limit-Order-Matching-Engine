package com.project.exchange.service;
 
import com.project.exchange.domain.Order;
import com.project.exchange.domain.OrderSide;
import com.project.exchange.domain.OrderStatus;
import com.project.exchange.matching.OrderBook;
import com.project.exchange.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
 
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
 
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;
 
@ExtendWith(MockitoExtension.class)
class RecoveryServiceTest {
 
    @Mock
    private OrderRepository orderRepository;
 
    private OrderBook orderBook;
    private RecoveryService recoveryService;
 
    @Captor
    private ArgumentCaptor<Collection<OrderStatus>> statusCaptor;
 
    @BeforeEach
    void setUp() {
        orderBook = new OrderBook();
        recoveryService = new RecoveryService(orderRepository, orderBook);
    }
 
    private Order createOrder(OrderSide side, BigDecimal price, Long originalQty, Long remainingQty, OrderStatus status, Long sequenceNumber) {
        return Order.builder()
                .id(UUID.randomUUID())
                .userId(1L)
                .instrument("ACME")
                .side(side)
                .limitPrice(price)
                .originalQuantity(originalQty)
                .remainingQuantity(remainingQty)
                .status(status)
                .sequenceNumber(sequenceNumber)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
 
    @Test
    @DisplayName("recover() fetches only OPEN and PARTIALLY_FILLED orders and rehydrates the OrderBook")
    void testRecoveryRehydratesActiveOrders() {
        // Arrange
        Order openBuy = createOrder(OrderSide.BUY, new BigDecimal("100.00"), 50L, 50L, OrderStatus.OPEN, 1L);
        Order partialBuy = createOrder(OrderSide.BUY, new BigDecimal("105.00"), 40L, 15L, OrderStatus.PARTIALLY_FILLED, 2L);
        Order openSell = createOrder(OrderSide.SELL, new BigDecimal("110.00"), 30L, 30L, OrderStatus.OPEN, 3L);
        Order partialSell = createOrder(OrderSide.SELL, new BigDecimal("115.00"), 25L, 10L, OrderStatus.PARTIALLY_FILLED, 4L);
 
        when(orderRepository.findByStatusInOrderBySequenceNumberAsc(anyCollection()))
                .thenReturn(List.of(openBuy, partialBuy, openSell, partialSell));
 
        // Act
        recoveryService.recover();
 
        // Assert: correct query was made
        verify(orderRepository).findByStatusInOrderBySequenceNumberAsc(statusCaptor.capture());
        assertThat(statusCaptor.getValue()).containsExactlyInAnyOrder(OrderStatus.OPEN, OrderStatus.PARTIALLY_FILLED);
 
        // Assert: OrderBook contains all active orders
        assertThat(orderBook.getBuyOrders()).hasSize(2);
        assertThat(orderBook.getSellOrders()).hasSize(2);
 
        // Assert: Buy queue has highest price first ($105 > $100)
        Order bestBuy = orderBook.getBestBuy();
        assertThat(bestBuy).isNotNull();
        assertThat(bestBuy.getLimitPrice()).isEqualByComparingTo(new BigDecimal("105.00"));
        assertThat(bestBuy.getRemainingQuantity()).isEqualTo(15L);
        assertThat(bestBuy.getStatus()).isEqualTo(OrderStatus.PARTIALLY_FILLED);
 
        // Assert: Sell queue has lowest price first ($110 < $115)
        Order bestSell = orderBook.getBestSell();
        assertThat(bestSell).isNotNull();
        assertThat(bestSell.getLimitPrice()).isEqualByComparingTo(new BigDecimal("110.00"));
        assertThat(bestSell.getRemainingQuantity()).isEqualTo(30L);
        assertThat(bestSell.getStatus()).isEqualTo(OrderStatus.OPEN);
    }
 
    @Test
    @DisplayName("recover() handles empty database gracefully with no orders rehydrated")
    void testRecoveryWithEmptyDatabase() {
        // Arrange
        when(orderRepository.findByStatusInOrderBySequenceNumberAsc(anyCollection()))
                .thenReturn(Collections.emptyList());
 
        // Act
        recoveryService.recover();
 
        // Assert
        assertThat(orderBook.getBuyOrders()).isEmpty();
        assertThat(orderBook.getSellOrders()).isEmpty();
        assertThat(orderBook.getBestBuy()).isNull();
        assertThat(orderBook.getBestSell()).isNull();
    }
 
    @Test
    @DisplayName("recover() preserves priority when prices are identical by sequence number")
    void testRecoveryPreservesSequenceNumberPriority() {
        // Arrange: two buy orders with the exact same price, but different sequence numbers
        Order earlierBuy = createOrder(OrderSide.BUY, new BigDecimal("100.00"), 10L, 10L, OrderStatus.OPEN, 10L);
        Order laterBuy = createOrder(OrderSide.BUY, new BigDecimal("100.00"), 20L, 20L, OrderStatus.OPEN, 20L);
 
        when(orderRepository.findByStatusInOrderBySequenceNumberAsc(anyCollection()))
                .thenReturn(List.of(earlierBuy, laterBuy));
 
        // Act
        recoveryService.recover();
 
        // Assert: earlier sequence number (10L) has priority over later (20L)
        assertThat(orderBook.getBestBuy().getSequenceNumber()).isEqualTo(10L);
    }
}
