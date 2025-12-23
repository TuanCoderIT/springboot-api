package com.example.springboot_api.dto.lecturer.chapter;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO cho thêm video YouTube vào chương.
 * Video sẽ được trích xuất phụ đề để tạo RAG chunks.
 */
@Data
public class ChapterYoutubeUploadRequest {

    /** URL video YouTube (bắt buộc) */
    @NotBlank(message = "URL video YouTube không được để trống")
    private String youtubeUrl;

    /** Tiêu đề hiển thị (optional - nếu null sẽ lấy từ video) */
    private String title;

    /** Mô tả ngắn (optional) */
    private String description;
}
