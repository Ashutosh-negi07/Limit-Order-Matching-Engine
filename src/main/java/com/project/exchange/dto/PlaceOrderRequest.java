package com.project.exchange.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.project.exchange.domain.OrderSide;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceOrderRequest {

    @NotNull(message = "userId is required")
    @Positive(message = "userId must be a positive number")
    private Long userId;

    @NotNull(message = "side is required")
    private OrderSide side;

    @NotNull(message = "limitPrice is required")
    @DecimalMin(value = "0.01", message = "limitPrice must be greater than zero")
    @JsonAlias({"price"})
    private BigDecimal limitPrice;

    @NotNull(message = "originalQuantity is required")
    @Positive(message = "originalQuantity must be greater than zero")
    @JsonAlias({"quantity"})
    private Long originalQuantity;

    @NotBlank(message = "instrument is required")
    @Pattern(regexp = "ACME", message = "instrument must be ACME")
    @Builder.Default
    private String instrument = "ACME";

}
