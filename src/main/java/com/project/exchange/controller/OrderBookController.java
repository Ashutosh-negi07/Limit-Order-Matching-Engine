package com.project.exchange.controller;

import com.project.exchange.dto.OrderBookResponse;
import com.project.exchange.dto.OrderResponse;
import com.project.exchange.mapper.OrderMapper;
import com.project.exchange.matching.OrderBook;
import com.project.exchange.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/orderbook", "/orderbook"})
@RequiredArgsConstructor
@Tag(name = "OrderBook", description = "Order book state snapshot")
public class OrderBookController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "View current order book", description = "Retrieves current active buy (highest price first) and sell (lowest price first) resting orders")
    @ApiResponse(responseCode = "200", description = "Order book snapshot")
    public ResponseEntity<OrderBookResponse> getOrderBook(){
        OrderBook book = orderService.getOrderBook();
        List<OrderResponse> buyOrders = book.getBuyOrders()
                .stream()
                .map(OrderMapper::toResponseDTO)
                .toList();
        List<OrderResponse> sellOrders = book.getSellOrders()
                .stream()
                .map(OrderMapper::toResponseDTO)
                .toList();
        OrderBookResponse response = OrderBookResponse.builder()
                .buyOrders(buyOrders)
                .sellOrders(sellOrders)
                .build();
        return ResponseEntity.ok(response);
    }

}
