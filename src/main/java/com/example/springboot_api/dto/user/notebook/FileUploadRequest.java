package com.example.springboot_api.dto.user.notebook;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class FileUploadRequest {

    // NOTE: notebookId đã được xóa vì nó sẽ được lấy từ @PathVariable

    @Min(value = 100, message = "Chunk size phải lớn hơn 100")
    @Max(value = 5000, message = "Chunk size không được vượt quá 5000")
    private Integer chunkSize = 800;

    @Min(value = 0, message = "Chunk overlap phải lớn hơn 0")
    @Max(value = 500, message = "Chunk overlap không được vượt quá 500")
    private Integer chunkOverlap = 120;
}