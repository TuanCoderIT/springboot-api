package com.example.springboot_api.dto.user.notebook;

import jakarta.validation.constraints.Size;

/**
 * Request để tạo notebook cá nhân.
 * 
 * Có 2 mode:
 * - autoGenerate = false (default): Nhập thủ công title + thumbnail bắt buộc
 * - autoGenerate = true: Chỉ cần description (≥10 từ), hệ thống tự tạo title và
 * thumbnail
 */
public record CreatePersonalNotebookRequest(
                @Size(max = 255, message = "Tiêu đề không được vượt quá 255 ký tự") String title,

                @Size(max = 5000, message = "Mô tả không được vượt quá 5000 ký tự") String description,

                Boolean autoGenerate) {
        /**
         * Kiểm tra xem có phải auto-generate mode không
         */
        public boolean isAutoGenerate() {
                return autoGenerate != null && autoGenerate;
        }
}
