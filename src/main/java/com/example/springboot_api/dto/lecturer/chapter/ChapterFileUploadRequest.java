package com.example.springboot_api.dto.lecturer.chapter;

import java.util.List;

import lombok.Data;

/**
 * DTO cho upload file vào chương.
 * Mỗi file có thể có title và description riêng.
 */
@Data
public class ChapterFileUploadRequest {

    /**
     * Danh sách metadata cho từng file (theo thứ tự files).
     * Có thể gửi dưới dạng JSON string.
     */
    private List<FileMetadata> fileInfos;

    @Data
    public static class FileMetadata {
        /** Tiêu đề hiển thị (optional - nếu null sẽ dùng tên file gốc) */
        private String title;

        /** Mô tả ngắn (optional) */
        private String description;
    }
}
