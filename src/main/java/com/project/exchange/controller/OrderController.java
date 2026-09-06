package com.project.exchange.controller;

import com.project.exchange.domain.Order;
import com.project.exchange.dto.OrderResponse;
import com.project.exchange.dto.PlaceOrderRequest;
import com.project.exchange.mapper.OrderMapper;
import com.project.exchange.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping({"/api/orders", "/orders"})
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order placement, inspection, and cancellation")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Place a new limit order", description = "Submits a buy or sell limit order for ACME and executes matching")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order placed and processed"),
            @ApiResponse(responseCode = "400", description = "Validation failure or malformed payload")
    })
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody PlaceOrderRequest request){
        Order order = OrderMapper.toOrderEntity(request);
        orderService.processOrder(order);
        OrderResponse response = OrderMapper.toResponseDTO(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "View order status", description = "Retrieves an order by its unique UUID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order retrieved"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable UUID id){
        Order order = orderService.getOrderById(id);
        OrderResponse response = OrderMapper.toResponseDTO(order);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel an active order", description = "Cancels an OPEN or PARTIALLY_FILLED order and removes it from the order book")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order cancelled successfully"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "409", description = "Conflict: cannot cancel FILLED or CANCELLED order")
    })
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable UUID id){
        Order order = orderService.cancelOrder(id);
        OrderResponse response = OrderMapper.toResponseDTO(order);
        return ResponseEntity.ok(response);
    }

}
