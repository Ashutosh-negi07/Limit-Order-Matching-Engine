package com.project.exchange.controller;

import com.project.exchange.dto.OrderBookResponse;
import com.project.exchange.dto.OrderResponse;
import com.project.exchange.mapper.OrderMapper;
import com.project.exchange.matching.OrderBook;
import com.project.exchange.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/orderbook")
@RequiredArgsConstructor
public class OrderBookController {

    private final OrderService orderService;

    @GetMapping
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
