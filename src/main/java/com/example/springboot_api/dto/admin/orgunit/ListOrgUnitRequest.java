package com.example.springboot_api.dto.admin.orgunit;

import lombok.Data;

/**
 * DTO filter danh sách đơn vị tổ chức.
 */
@Data
public class ListOrgUnitRequest {
    private Integer page = 0;
    private Integer size = 11;
    private String sortBy = "createdAt";
    private String sortDir = "desc";
    private String q; // Search query
    private String type; // Filter theo loại
    private Boolean isActive; // Filter theo trạng thái
}
