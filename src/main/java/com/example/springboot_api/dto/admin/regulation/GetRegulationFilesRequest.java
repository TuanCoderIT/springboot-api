package com.example.springboot_api.dto.admin.regulation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Request params để lấy danh sách regulation files.
 */
@Data
public class GetRegulationFilesRequest {

    @Min(value = 0, message = "Page phải >= 0")
    private Integer page = 0;

    @Min(value = 1, message = "Size phải >= 1")
    @Max(value = 100, message = "Size phải <= 100")
    private Integer size = 20;

    @Pattern(regexp = "^(originalFilename|fileSize|createdAt|updatedAt)$", message = "SortBy phải là: originalFilename, fileSize, createdAt, updatedAt")
    private String sortBy = "createdAt";

    @Pattern(regexp = "^(asc|desc)$", message = "SortDirection phải là: asc, desc")
    private String sortDirection = "desc";

    private String search; // Search in filename

    @Pattern(regexp = "^(pending|approved|processing|failed)$", message = "Status phải là: pending, approved, processing, failed")
    private String status;
}
