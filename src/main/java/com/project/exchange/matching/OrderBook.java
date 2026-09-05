package com.project.exchange.matching;

import com.project.exchange.domain.Order;
import com.project.exchange.domain.OrderSide;

import java.util.*;

public class OrderBook {

    private PriorityQueue<Order> buyBook = new PriorityQueue<>(new BuyOrderComparator());
    private PriorityQueue<Order> sellBook = new PriorityQueue<>(new SellOrderComparator());

    public void addOrder(Order order){
        if(order.getSide()== OrderSide.BUY){
            buyBook.add(order);
        }else{sellBook.add(order);}
    }

    public void removeOrder(Order order){
        if(order.getSide()== OrderSide.BUY){
            buyBook.remove(order);
        }else{sellBook.remove(order);}
    }

    public Order getBestBuy(){
        return buyBook.peek();
    }

    public Order getBestSell(){
        return sellBook.peek();
    }

    public List<Order> getBuyOrders(){
        return  new ArrayList<>(buyBook);
    }

    public List<Order> getSellOrders(){
        return  new ArrayList<>(sellBook);
    }



}
