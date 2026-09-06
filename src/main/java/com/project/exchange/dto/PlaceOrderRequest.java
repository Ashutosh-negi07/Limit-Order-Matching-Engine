package com.project.exchange.dto;

import com.project.exchange.domain.OrderSide;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PlaceOrderRequest {

    @NotNull(message = "userId is required")
    @Positive(message = "userId must be a positive number")
    Long userId;

    @NotNull(message = "side is required")
    OrderSide side;

    @NotNull(message = "limitPrice is required")
    @DecimalMin(value = "0.01", message = "limitPrice must be greater than zero")
    BigDecimal limitPrice;

    @NotNull(message = "originalQuantity is required")
    @Positive(message = "originalQuantity must be greater than zero")
    Long originalQuantity;

    @NotBlank(message = "instrument is required")
    @Pattern(regexp = "ACME", message = "instrument must be ACME")
    String instrument;

    @NotNull(message = "sequenceNumber is required")
    @Positive(message = "sequenceNumber must be a positive number")
    Long sequenceNumber;
}
