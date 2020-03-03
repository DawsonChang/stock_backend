package com.pl3.idbc;

import java.sql.Connection;
import java.sql.DriverManager;

public class JDBCTest {
    public static void main(String[] args) {
        String jdbcUrl = "jdbc:mysql://localhost:3306/pl3?useSSL=false&serverTimezone=UTC";
        String user = "tester1";
        String pass = "tester.1";
        try {
            Connection myConn = DriverManager.getConnection(jdbcUrl, user, pass);
            System.out.println("Connection successful!!!");
        }
        catch (Exception exc) {
            exc.printStackTrace();
        }
    }
}

