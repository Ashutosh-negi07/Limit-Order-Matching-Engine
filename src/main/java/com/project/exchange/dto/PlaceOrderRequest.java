package com.project.exchange.dto;

import com.project.exchange.domain.OrderSide;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
public class PlaceOrderRequest {


    Long userId;



    OrderSide side;

    BigDecimal limitPrice;

    Long originalQuantity;





    String instrument;



    Long sequenceNumber;
}
