package com.project.exchange.mapper;

import com.project.exchange.domain.Trade;
import com.project.exchange.dto.TradeResponse;

public class TradeMapper {

    public static TradeResponse toResponseDTO(Trade trade) {
        return TradeResponse.builder()
                .id(trade.getId())
                .buyOrderId(trade.getBuyOrderId())
                .sellOrderId(trade.getSellOrderId())
                .instrument(trade.getInstrument())
                .executionPrice(trade.getExecutionPrice())
                .quantity(trade.getQuantity())
                .executedAt(trade.getExecutedAt())
                .build();
    }
}
