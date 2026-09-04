package com.project.exchange.matching;

import com.project.exchange.domain.Order;

import java.util.Comparator;

public class BuyOrderComparator implements Comparator<Order> {

    @Override
    public int compare(Order o1, Order o2) {

        int priceComparison =
                o1.getLimitPrice().compareTo(o2.getLimitPrice());

        if (priceComparison > 0) {
            return -1; // higher price first
        } else if (priceComparison < 0) {
            return 1;
        }

        int timeComparison =
                o1.getCreatedAt().compareTo(o2.getCreatedAt());

        if (timeComparison < 0) {
            return -1; // earlier order first
        } else if (timeComparison > 0) {
            return 1;
        } else {
            return 0;
        }
    }
}

