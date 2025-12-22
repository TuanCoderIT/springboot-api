package com.example.springboot_api.dto.admin.lecturer;

import java.util.UUID;

import lombok.Data;

@Data
public class ListLecturerRequest {
    private String q = "";
    private Boolean active;
    private UUID orgUnitId; // Filter theo đơn vị tổ chức
    private int page = 0;
    private int size = 10;
    private String sortBy;
    private String sortDir;
}
