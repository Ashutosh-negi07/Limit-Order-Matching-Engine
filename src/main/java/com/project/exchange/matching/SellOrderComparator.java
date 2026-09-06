package com.project.exchange.matching;


import com.project.exchange.domain.Order;

import java.util.Comparator;

public class SellOrderComparator implements Comparator<Order> {

    @Override
    public int compare(Order o1, Order o2){
        int priceComparison =
                o1.getLimitPrice().compareTo(o2.getLimitPrice());
        if(priceComparison>0){
            return 1;
        }else if(priceComparison<0){
            return -1;
        }

        if (o1.getSequenceNumber() != null && o2.getSequenceNumber() != null) {
            return o1.getSequenceNumber().compareTo(o2.getSequenceNumber());
        } else if (o1.getCreatedAt() != null && o2.getCreatedAt() != null) {
            return o1.getCreatedAt().compareTo(o2.getCreatedAt());
        } else {
            return 0;
        }

    }
}
