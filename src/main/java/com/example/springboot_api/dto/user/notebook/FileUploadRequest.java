package com.example.springboot_api.dto.user.notebook;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class FileUploadRequest {

    // NOTE: notebookId đã được xóa vì nó sẽ được lấy từ @PathVariable

    @Min(value = 3000, message = "Chunk size phải lớn hơn hoặc bằng 3000")
    @Max(value = 5000, message = "Chunk size không được vượt quá 5000")
    private Integer chunkSize = 3000;

    @Min(value = 200, message = "Chunk overlap phải lớn hơn hoặc bằng 200")
    @Max(value = 500, message = "Chunk overlap không được vượt quá 500")
    private Integer chunkOverlap = 250;
}