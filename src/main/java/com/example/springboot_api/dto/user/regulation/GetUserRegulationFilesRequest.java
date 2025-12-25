package com.example.springboot_api.dto.user.regulation;

import lombok.Data;

/**
 * Request params cho lấy danh sách file quy chế (user side).
 */
@Data
public class GetUserRegulationFilesRequest {
    private Integer page = 0;
    private Integer size = 10;
    private String search;
    private String sortBy = "createdAt";
    private String sortDirection = "desc";
}
