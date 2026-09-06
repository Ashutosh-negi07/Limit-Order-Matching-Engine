package com.project.exchange.mapper;


import com.project.exchange.domain.Order;
import com.project.exchange.domain.OrderStatus;
import com.project.exchange.dto.OrderResponse;
import com.project.exchange.dto.PlaceOrderRequest;

import java.time.Instant;

public class OrderMapper {

    public static Order toOrderEntity(PlaceOrderRequest request){
        return Order.builder()
                .userId(request.getUserId())
                .instrument(request.getInstrument())
                .side(request.getSide())
                .limitPrice(request.getLimitPrice())
                .originalQuantity(request.getOriginalQuantity())
                .remainingQuantity(request.getOriginalQuantity())
                .status(OrderStatus.OPEN)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
    public static OrderResponse toResponseDTO(Order order){
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .instrument(order.getInstrument())
                .side(order.getSide())
                .status(order.getStatus())
                .limitPrice(order.getLimitPrice())
                .originalQuantity(order.getOriginalQuantity())
                .remainingQuantity(order.getRemainingQuantity())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .sequenceNumber(order.getSequenceNumber())
                .build();
    }

}
