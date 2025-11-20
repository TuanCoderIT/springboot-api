package com.example.springboot_api;

import java.sql.Connection;
import java.sql.DriverManager;

public class TestDbConnection {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/notebook";
        String user = "postgres";
        String password = "88888888";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("Kết nối OK!");
        } catch (Exception e) {
            System.out.println("Kết nối FAIL: " + e.getMessage());
        }
    }
}
