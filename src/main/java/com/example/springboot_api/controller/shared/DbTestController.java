package com.example.springboot_api.controller;

import java.sql.Connection;

import javax.sql.DataSource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DbTestController {

    private final DataSource dataSource;

    public DbTestController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/db-test")
    public String testConnection() {
        try (Connection conn = dataSource.getConnection()) {
            return "KẾT NỐI OK: " + conn.getMetaData().getURL();
        } catch (Exception e) {
            return "Kết nối lỗi: " + e;
        }
    }
}
