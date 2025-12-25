package com.example.springboot_api.dto.admin.regulation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * Request để upload file quy chế.
 */
@Data
public class RegulationFileUploadRequest {

    @Min(value = 500, message = "Chunk size phải >= 500")
    @Max(value = 2000, message = "Chunk size phải <= 2000")
    private Integer chunkSize = 800;

    @Min(value = 50, message = "Chunk overlap phải >= 50")
    @Max(value = 400, message = "Chunk overlap phải <= 400")
    private Integer chunkOverlap = 120;
}
