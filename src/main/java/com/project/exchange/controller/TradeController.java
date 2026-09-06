package com.project.exchange.controller;

import com.project.exchange.mapper.TradeMapper;
import com.project.exchange.dto.TradeResponse;
import com.project.exchange.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/trades")
@RequiredArgsConstructor
public class TradeController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<List<TradeResponse>> getAllTrades(){
        return ResponseEntity.ok(orderService.getAllTrades().stream().map(trade -> TradeMapper.toResponseDTO(trade)).toList());
    }
}
