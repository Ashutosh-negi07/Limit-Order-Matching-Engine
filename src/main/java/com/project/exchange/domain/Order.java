package com.project.exchange.domain;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue
    @Column(nullable = false)
    private UUID id;

    @Column(name = "user_id",nullable = false)
    private Long userId;

    @Column(name = "instrument",nullable = false)
    private String instrument;

    @Enumerated(EnumType.STRING)
    @Column(name = "side",nullable = false)
    private OrderSide side;

    @Column(name = "limit_price", nullable = false)
    private BigDecimal limitPrice;

    @Column(nullable = false)
    private Long originalQuantity;

    @Column(nullable = false)
    private Long remainingQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private Long sequenceNumber;//For ordering during restart recovery


}
