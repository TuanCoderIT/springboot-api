package com.example.springboot_api.dto.admin.user;

import lombok.Data;

@Data
public class ListUserRequest {
    private String q = "";
    private String role;
    private int page = 0;
    private int size = 11;
    private String sortBy;
    private String sortDir;
}
