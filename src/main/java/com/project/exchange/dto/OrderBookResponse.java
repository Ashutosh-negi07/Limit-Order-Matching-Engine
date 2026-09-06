package com.project.exchange.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class OrderBookResponse {
    List<OrderResponse> buyOrders;
    List<OrderResponse> sellOrders;
}
