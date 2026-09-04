package com.project.exchange.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trade {

    @Id
    @GeneratedValue
    @Column(nullable = false)
    private UUID id;

    @Column(name = "buy_order_id", nullable = false)
    private UUID buyOrderId;

    @Column(name = "sell_order_id", nullable = false)
    private UUID sellOrderId;

    @Column(name = "instrument", nullable = false)
    private String instrument;

    @Column(name = "execution_price", nullable = false)
    private BigDecimal executionPrice;

    @Column(nullable = false)
    private Long quantity;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;
}
