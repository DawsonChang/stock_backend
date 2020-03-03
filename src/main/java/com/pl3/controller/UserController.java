package com.pl3.controller;


import com.pl3.helper.QuoteBySymbol;
import com.pl3.model.Pair;
import com.pl3.model.Stock;
import com.pl3.model.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class UserController {
    SessionFactory factory;
    Session session;
    String q1 = "";
    String q2 = "";

    @GetMapping("/")
    public Pair getAllStocksAndUser(String currentUsername) throws IOException {
        System.out.println(currentUsername);

        factory = new Configuration().configure("hibernate.cfg.xml")
                .addAnnotatedClass(User.class)
                .addAnnotatedClass(Stock.class)
                .buildSessionFactory();
        session = factory.getCurrentSession();
        session.beginTransaction();

        List<Stock> stockList = new ArrayList<>();
        q1 = "FROM Stock s WHERE s.username = :username";
        stockList = session.createQuery(q1).setParameter("username", currentUsername).getResultList();

        User user = new User();
        q2 = "FROM User u WHERE u.username = :username";
        user = (User)session.createQuery(q2).setParameter("username", currentUsername).uniqueResult();

        QuoteBySymbol quote;
        JSONObject json;

        for (Stock s: stockList) {
            quote = new QuoteBySymbol(s.getSymbol());
            json = quote.readJsonFromUrl();
            s.setPrice(((Number) json.getJSONObject("quote").get("latestPrice")).doubleValue());
            s.setPrice(Math.round(s.getPrice()*100.0)/100.0);
            s.setTotal(Math.round(s.getShares()*s.getPrice() * 100.0)/100.0);
        }

        session.getTransaction().commit();
        System.out.println("Done");
        factory.close();

        // return Pair because I want to return both User and List<Stock> data structure
        Pair pair = new Pair(user, stockList);
        return pair;
    }

    @PostMapping("/quote")
    Stock Search(@RequestBody Stock stock) throws IOException {
        System.out.println("The symbol is: " + stock.getSymbol());
        QuoteBySymbol quote = new QuoteBySymbol(stock.getSymbol());
        JSONObject json = quote.readJsonFromUrl();

        if (json == null) {
            return null;
        }

        System.out.println(json.toString());

        stock.setName(json.getJSONObject("quote").getString("companyName"));
        stock.setPrice(((Number)json.getJSONObject("quote").get("latestPrice")).doubleValue());
        stock.setPrice(Math.round(stock.getPrice()*100.0)/100.0);

        System.out.println(stock.getName());
        System.out.println(stock.getPrice());

        return stock;
    }

    @PostMapping("/buy")
    int buyStock(@RequestBody Stock stock) throws IOException {

        factory = new Configuration().configure("hibernate.cfg.xml")
                .addAnnotatedClass(User.class)
                .addAnnotatedClass(Stock.class)
                .buildSessionFactory();
        session = factory.getCurrentSession();
        session.beginTransaction();

        String currentUsername = stock.getUsername();
        Stock dbStock = new Stock();
        // search for the stock is existing or not
        q1 = "FROM Stock s WHERE s.symbol = :symbol and s.username = :username";
        dbStock = (Stock)session.createQuery(q1).setParameter("symbol", stock.getSymbol()).setParameter("username", currentUsername).uniqueResult();

        User user = new User();
        q2 = "FROM User u WHERE u.username = :username";
        user = (User)session.createQuery(q2).setParameter("username", currentUsername).uniqueResult();

        // get latest price and set it as cost
        QuoteBySymbol quote = new QuoteBySymbol(stock.getSymbol());
        JSONObject json = quote.readJsonFromUrl();
        stock.setCost(((Number)json.getJSONObject("quote").get("latestPrice")).doubleValue());
        stock.setCost(Math.round(stock.getCost()*100.0)/100.0);

        // if the total cost of stock is greater than the number of cash
        if (stock.getShares() * stock.getCost() > user.getCash()) {
            System.out.println("cash is not enough, return 1");
            return 1;
        }

        // if cannot get current symbol from database
        if (dbStock == null) {
            stock.setSymbol(stock.getSymbol().toUpperCase());
            stock.setUsername(currentUsername);
            System.out.println("*****create*****");
            session.save(stock);
        }
        // if got current symbol from database
        else {
            // new shares is equal to old shares + adding shares
            dbStock.setShares(dbStock.getShares() + stock.getShares());

            // average the cost
            dbStock.setCost((dbStock.getShares() * dbStock.getCost() + stock.getShares() * stock.getCost()) /
                    (dbStock.getShares() + stock.getShares()));

            System.out.println("*****update*****");
            session.update(dbStock);
        }

        user.setCash(user.getCash() - (stock.getCost()*stock.getShares()));
        session.update(user);
        session.getTransaction().commit();
        System.out.println("Done!");
        factory.close();
        System.out.println("return 0");
        return 0;
    }

    @GetMapping("/sell")
    List<Stock> getAllStocks(String currentUsername) throws IOException {
        factory = new Configuration().configure("hibernate.cfg.xml")
                .addAnnotatedClass(User.class)
                .addAnnotatedClass(Stock.class)
                .buildSessionFactory();
        session = factory.getCurrentSession();
        session.beginTransaction();

        List<Stock> stockList = new ArrayList<>();
        q1 = "FROM Stock s WHERE s.username = :username";
        stockList = session.createQuery(q1).setParameter("username", currentUsername).getResultList();

        session.getTransaction().commit();
        System.out.println("Done");
        factory.close();

        return stockList;
    }

    @PostMapping("/sell")
    void sellStock(@RequestBody Stock stock) throws IOException {
        factory = new Configuration().configure("hibernate.cfg.xml")
                .addAnnotatedClass(User.class)
                .addAnnotatedClass(Stock.class)
                .buildSessionFactory();
        session = factory.getCurrentSession();
        session.beginTransaction();

        String currentUsername = stock.getUsername();
        Stock dbStock = new Stock();
        // select current stock in db
        q1 = "FROM Stock s WHERE s.symbol = :symbol and s.username = :username";
        dbStock = (Stock)session.createQuery(q1).setParameter("symbol", stock.getSymbol()).setParameter("username", currentUsername).uniqueResult();

        // select current user in db
        User user = new User();
        q2 = "FROM User u WHERE u.username = :username";
        user = (User)session.createQuery(q2).setParameter("username", currentUsername).uniqueResult();

        // get latest price and set it as cost
        QuoteBySymbol quote = new QuoteBySymbol(stock.getSymbol());
        JSONObject json = quote.readJsonFromUrl();
        stock.setCost(((Number)json.getJSONObject("quote").get("latestPrice")).doubleValue());
        stock.setCost(Math.round(stock.getCost()*100.0)/100.0);

        // if the shares of stock in db is equal the shares of stock to be sold, delete the stock row in db and increase the cash
        if(dbStock.getShares() == stock.getShares()) {
            System.out.println("*****delete*****");
            session.delete(dbStock);
        }
        // else, decrease the shares of stock in db, and increase the cash
        else {
            // new shares is equal to old shares + adding shares
            dbStock.setShares(dbStock.getShares() - stock.getShares());

            System.out.println("*****update*****");
            session.update(dbStock);
        }

        user.setCash(user.getCash() + (stock.getPrice() * stock.getShares()));
        session.update(user);
        session.getTransaction().commit();
        System.out.println("Done!");
        factory.close();
    }

    @PostMapping("/login")
    Boolean checkLogin(@RequestBody String[] userPair) throws IOException {
        factory = new Configuration().configure("hibernate.cfg.xml")
                .addAnnotatedClass(User.class)
                .addAnnotatedClass(Stock.class)
                .buildSessionFactory();
        session = factory.getCurrentSession();
        session.beginTransaction();

        String inputUsername = userPair[0];
        String inputPassword = userPair[1];
        System.out.println(inputUsername);
        System.out.println(inputPassword);

        // select current user in db
        User user = new User();
        q2 = "FROM User u WHERE u.username = :username";
        user = (User)session.createQuery(q2).setParameter("username", inputUsername).uniqueResult();

        session.getTransaction().commit();
        System.out.println("Done!");
        factory.close();

        if (user == null) {
            System.out.println("user == null");
            return false;
        }
        if (!user.getPassword().equals(inputPassword)) {
            System.out.println("pass not match");
            return false;
        }
        return true;
    }

    @PostMapping("/register")
    int checkRegister(@RequestBody String[] userPair) throws IOException {
        factory = new Configuration().configure("hibernate.cfg.xml")
                .addAnnotatedClass(User.class)
                .addAnnotatedClass(Stock.class)
                .buildSessionFactory();
        session = factory.getCurrentSession();
        session.beginTransaction();

        String inputUsername = userPair[0];
        String inputPassword = userPair[1];
        System.out.println(inputUsername);
        System.out.println(inputPassword);

        // select current user in db
        User user = new User();
        q2 = "FROM User u WHERE u.username = :username";
        user = (User)session.createQuery(q2).setParameter("username", inputUsername).uniqueResult();

        // if user does not exist in db, create a new one
        if (user == null) {
            System.out.println("user == null");
            user = new User();
            user.setUsername(inputUsername);
            user.setPassword(inputPassword);
            user.setCash(10000);
            session.save(user);
            session.getTransaction().commit();
            System.out.println("Done!");
            factory.close();
            return 0;
        }
        // if user exists in db, do nothing and return 1
        else {
            session.getTransaction().commit();
            System.out.println("Done!");
            factory.close();
            return 1;
        }
    }
}
