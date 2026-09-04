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

            int timeComparison = o1.getCreatedAt().compareTo(o2.getCreatedAt());
            if(timeComparison>0){
                return 1;
            }
            else if(timeComparison<0){
                return -1;
            }else{
                return 0;
            }

    }
}
