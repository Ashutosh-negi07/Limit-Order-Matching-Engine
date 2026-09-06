package com.project.exchange.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.exchange.domain.Order;
import com.project.exchange.domain.OrderSide;
import com.project.exchange.domain.OrderStatus;
import com.project.exchange.domain.Trade;
import com.project.exchange.dto.PlaceOrderRequest;
import com.project.exchange.exception.GlobalExceptionHandler;
import com.project.exchange.matching.OrderBook;
import com.project.exchange.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {OrderController.class, OrderBookController.class, TradeController.class})
@Import(GlobalExceptionHandler.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @Test
    @DisplayName("POST /api/orders: Successful order creation returns 201 CREATED")
    void testCreateOrderSuccess() throws Exception {
        UUID orderId = UUID.randomUUID();
        doAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(orderId);
            order.setSequenceNumber(1L);
            return null;
        }).when(orderService).processOrder(any(Order.class));

        PlaceOrderRequest request = PlaceOrderRequest.builder()
                .userId(1L)
                .side(OrderSide.BUY)
                .limitPrice(new BigDecimal("50.00"))
                .originalQuantity(10L)
                .instrument("ACME")
                .build();

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.side").value("BUY"))
                .andExpect(jsonPath("$.limitPrice").value(50.00))
                .andExpect(jsonPath("$.originalQuantity").value(10))
                .andExpect(jsonPath("$.remainingQuantity").value(10))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.instrument").value("ACME"));
    }

    @Test
    @DisplayName("POST /api/orders: Accepts price and quantity aliases, defaults instrument to ACME")
    void testCreateOrderWithAliasesAndDefaultInstrument() throws Exception {
        UUID orderId = UUID.randomUUID();
        doAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(orderId);
            order.setSequenceNumber(1L);
            return null;
        }).when(orderService).processOrder(any(Order.class));

        String jsonPayload = """
                {
                  "userId": 1,
                  "side": "BUY",
                  "price": 50.00,
                  "quantity": 10
                }
                """;

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.instrument").value("ACME"))
                .andExpect(jsonPath("$.limitPrice").value(50.00))
                .andExpect(jsonPath("$.originalQuantity").value(10));
    }

    @Test
    @DisplayName("POST /api/orders: Invalid price (<= 0) returns 400 Bad Request")
    void testCreateOrderInvalidPrice() throws Exception {
        String jsonPayload = """
                {
                  "userId": 1,
                  "side": "BUY",
                  "price": 0.00,
                  "quantity": 10
                }
                """;

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.limitPrice").exists());
    }

    @Test
    @DisplayName("POST /api/orders: Invalid quantity (<= 0) returns 400 Bad Request")
    void testCreateOrderInvalidQuantity() throws Exception {
        String jsonPayload = """
                {
                  "userId": 1,
                  "side": "BUY",
                  "price": 50.00,
                  "quantity": -5
                }
                """;

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.originalQuantity").exists());
    }

    @Test
    @DisplayName("POST /api/orders: Invalid side returns 400 Bad Request")
    void testCreateOrderInvalidSide() throws Exception {
        String jsonPayload = """
                {
                  "userId": 1,
                  "side": "HOLD",
                  "price": 50.00,
                  "quantity": 10
                }
                """;

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("side")));
    }

    @Test
    @DisplayName("GET /api/orders/{id}: Successful order fetch returns 200 OK")
    void testGetOrderSuccess() throws Exception {
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder()
                .id(orderId)
                .userId(1L)
                .instrument("ACME")
                .side(OrderSide.BUY)
                .limitPrice(new BigDecimal("50.00"))
                .originalQuantity(10L)
                .remainingQuantity(10L)
                .status(OrderStatus.OPEN)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .sequenceNumber(1L)
                .build();

        when(orderService.getOrderById(orderId)).thenReturn(order);

        mockMvc.perform(get("/api/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    @DisplayName("GET /api/orders/{id}: Unknown order returns 404 Not Found")
    void testGetOrderNotFound() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.getOrderById(orderId))
                .thenThrow(new NoSuchElementException("Order not found with id: " + orderId));

        mockMvc.perform(get("/api/orders/" + orderId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("Order not found")));
    }

    @Test
    @DisplayName("DELETE /api/orders/{id}: Successful cancellation returns 200 OK")
    void testCancelOrderSuccess() throws Exception {
        UUID orderId = UUID.randomUUID();
        Order cancelled = Order.builder()
                .id(orderId)
                .userId(1L)
                .instrument("ACME")
                .side(OrderSide.BUY)
                .limitPrice(new BigDecimal("50.00"))
                .originalQuantity(10L)
                .remainingQuantity(10L)
                .status(OrderStatus.CANCELLED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .sequenceNumber(1L)
                .build();

        when(orderService.cancelOrder(orderId)).thenReturn(cancelled);

        mockMvc.perform(delete("/api/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("DELETE /api/orders/{id}: Cancelling filled or cancelled order returns 409 Conflict")
    void testCancelFilledOrderConflict() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderService.cancelOrder(orderId))
                .thenThrow(new IllegalStateException("Cannot cancel order with status: FILLED"));

        mockMvc.perform(delete("/api/orders/" + orderId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("Cannot cancel order with status: FILLED")));
    }

    @Test
    @DisplayName("GET /api/orderbook: Returns 200 OK with active buy and sell orders")
    void testGetOrderBook() throws Exception {
        OrderBook book = new OrderBook();
        Order buy = Order.builder()
                .id(UUID.randomUUID())
                .userId(1L)
                .instrument("ACME")
                .side(OrderSide.BUY)
                .limitPrice(new BigDecimal("50.00"))
                .originalQuantity(10L)
                .remainingQuantity(10L)
                .status(OrderStatus.OPEN)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .sequenceNumber(1L)
                .build();
        Order sell = Order.builder()
                .id(UUID.randomUUID())
                .userId(2L)
                .instrument("ACME")
                .side(OrderSide.SELL)
                .limitPrice(new BigDecimal("55.00"))
                .originalQuantity(5L)
                .remainingQuantity(5L)
                .status(OrderStatus.OPEN)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .sequenceNumber(2L)
                .build();
        book.addOrder(buy);
        book.addOrder(sell);

        when(orderService.getOrderBook()).thenReturn(book);

        mockMvc.perform(get("/api/orderbook"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buyOrders", hasSize(1)))
                .andExpect(jsonPath("$.buyOrders[0].limitPrice").value(50.00))
                .andExpect(jsonPath("$.sellOrders", hasSize(1)))
                .andExpect(jsonPath("$.sellOrders[0].limitPrice").value(55.00));
    }

    @Test
    @DisplayName("GET /api/trades: Returns 200 OK with trade history")
    void testGetAllTrades() throws Exception {
        Trade trade = Trade.builder()
                .id(UUID.randomUUID())
                .buyOrderId(UUID.randomUUID())
                .sellOrderId(UUID.randomUUID())
                .instrument("ACME")
                .executionPrice(new BigDecimal("50.00"))
                .quantity(10L)
                .executedAt(Instant.now())
                .build();

        when(orderService.getAllTrades()).thenReturn(List.of(trade));

        mockMvc.perform(get("/api/trades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].executionPrice").value(50.00))
                .andExpect(jsonPath("$[0].quantity").value(10))
                .andExpect(jsonPath("$[0].instrument").value("ACME"));
    }
}
