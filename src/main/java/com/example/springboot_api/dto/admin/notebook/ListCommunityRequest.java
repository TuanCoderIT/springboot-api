package com.example.springboot_api.dto.admin.notebook;

import lombok.Data;

@Data
public class ListCommunityRequest {
    private String q = ""; // Tìm kiếm theo title, description
    private String visibility; // Filter theo visibility (public/private)
    private int page = 0;
    private int size = 10;
    private String sortBy; // createdAt, title, memberCount
    private String sortDir; // asc, desc
}

