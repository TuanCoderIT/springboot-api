package com.example.springboot_api.dto.admin.subject;

import java.util.UUID;

import lombok.Data;

@Data
public class ListSubjectRequest {
    private String q;
    private Boolean isActive;
    private UUID majorId;
    private int page = 0;
    private int size = 10;
    private String sortBy = "code";
    private String sortDir = "asc";
}
