package com.example.springboot_api.dto.admin.term;

import lombok.Data;

/**
 * Request filter và phân trang danh sách Term.
 */
@Data
public class ListTermRequest {
    private String q = "";           // Search theo code hoặc name
    private Boolean isActive;        // Filter theo trạng thái active
    private int page = 0;
    private int size = 10;
    private String sortBy = "createdAt";
    private String sortDir = "desc";
}
