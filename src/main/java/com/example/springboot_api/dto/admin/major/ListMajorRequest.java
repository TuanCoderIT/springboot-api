package com.example.springboot_api.dto.admin.major;

import java.util.UUID;

import lombok.Data;

/**
 * Request filter và phân trang danh sách Major.
 */
@Data
public class ListMajorRequest {
    private String q = ""; // Search theo code hoặc name
    private Boolean isActive; // Filter theo trạng thái active
    private UUID orgUnitId; // Filter theo đơn vị tổ chức
    private int page = 0;
    private int size = 10;
    private String sortBy = "code"; // Default sort theo code
    private String sortDir = "asc";
}
