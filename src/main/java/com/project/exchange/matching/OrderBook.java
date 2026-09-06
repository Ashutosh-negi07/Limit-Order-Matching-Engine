package com.project.exchange.matching;

import com.project.exchange.domain.Order;
import com.project.exchange.domain.OrderSide;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class OrderBook {

    private PriorityQueue<Order> buyBook = new PriorityQueue<>(new BuyOrderComparator());
    private PriorityQueue<Order> sellBook = new PriorityQueue<>(new SellOrderComparator());

    public void addOrder(Order order){
        if(order.getSide()== OrderSide.BUY){
            buyBook.add(order);
        }else{sellBook.add(order);}
    }

    public boolean removeOrderById(UUID orderId, OrderSide side){
        if (side == OrderSide.BUY) {
            return buyBook.removeIf(order -> Objects.equals(order.getId(), orderId));
        }
        if (side == OrderSide.SELL) {
            return sellBook.removeIf(order -> Objects.equals(order.getId(), orderId));
        }
        return false;

    }

    public Order getBestBuy(){
        return buyBook.peek();
    }

    public Order getBestSell(){
        return sellBook.peek();
    }

    public List<Order> getBuyOrders(){
        List<Order> list = new ArrayList<>(buyBook);
        list.sort(new BuyOrderComparator());
        return list;
    }

    public List<Order> getSellOrders(){
        List<Order> list = new ArrayList<>(sellBook);
        list.sort(new SellOrderComparator());
        return list;
    }



}
