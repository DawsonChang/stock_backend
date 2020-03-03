package com.pl3.model;

import java.util.List;

public class Pair {
    User user;
    List<Stock> stocks;

    public Pair(User user, List<Stock> stocks) {
        this.user = user;
        this.stocks = stocks;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<Stock> getStocks() {
        return stocks;
    }

    public void setStocks(List<Stock> stocks) {
        this.stocks = stocks;
    }

    @Override
    public String toString() {
        return "Pair{" +
                "user=" + user +
                ", stocks=" + stocks +
                '}';
    }
}
