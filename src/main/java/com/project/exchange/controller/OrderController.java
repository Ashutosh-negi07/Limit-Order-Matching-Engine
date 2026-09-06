package com.project.exchange.controller;

import com.project.exchange.domain.Order;
import com.project.exchange.dto.OrderMapper;
import com.project.exchange.dto.OrderResponse;
import com.project.exchange.dto.PlaceOrderRequest;
import com.project.exchange.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;


    @PostMapping()
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody PlaceOrderRequest request){
        Order order = OrderMapper.toOrderEntity(request);

        orderService.processOrder(order);
        OrderResponse response = OrderMapper.toResponseDTO(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable UUID id){
        Order order = orderService.getOrderById(id);
        OrderResponse response = OrderMapper.toResponseDTO(order);
        return ResponseEntity.ok(response);



    }




}
