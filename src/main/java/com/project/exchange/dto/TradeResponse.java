package com.project.exchange.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class TradeResponse {

    private UUID id;
    private UUID buyOrderId;
    private UUID sellOrderId;
    private String instrument;
    private BigDecimal executionPrice;
    private Long quantity;
    private Instant executedAt;
}
