package com.project.exchange.matching;

import com.project.exchange.domain.Order;
import com.project.exchange.domain.Trade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;


import java.util.List;
//this is kinda like a dto

@Getter
@AllArgsConstructor
@Builder
public class MatchResult {

    private final Order updatedIncomingOrder;
    private final List<Order> updatedOppositeOrders;
    private final List<Trade> createdTrades;

}
