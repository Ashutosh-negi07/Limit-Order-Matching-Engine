package com.project.exchange.controller;

import com.project.exchange.dto.TradeResponse;
import com.project.exchange.mapper.TradeMapper;
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
@RequestMapping({"/api/trades", "/trades"})
@RequiredArgsConstructor
@Tag(name = "Trades", description = "Trade execution history")
public class TradeController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "View trade history", description = "Returns the chronological list of all executed trades")
    @ApiResponse(responseCode = "200", description = "List of executed trades")
    public ResponseEntity<List<TradeResponse>> getAllTrades(){
        return ResponseEntity.ok(orderService.getAllTrades().stream().map(TradeMapper::toResponseDTO).toList());
    }

}
