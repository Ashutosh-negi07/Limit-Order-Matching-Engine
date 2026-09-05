package com.project.exchange.dto;

import com.project.exchange.domain.OrderSide;
import com.project.exchange.domain.OrderStatus;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class OrderResponse {

    UUID id;

    Long userId;

    String instrument;

    OrderSide side;

    BigDecimal limitPrice;

    Long originalQuantity;

    Long remainingQuantity;

    OrderStatus status;

    Instant createdAt;

    Instant updatedAt;

    Long sequenceNumber;
}
