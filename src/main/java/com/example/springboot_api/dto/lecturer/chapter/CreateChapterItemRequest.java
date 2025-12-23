package com.example.springboot_api.dto.lecturer.chapter;

import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO để tạo ChapterItem (không phải FILE).
 * Dùng cho LECTURE, QUIZ, NOTE, VIDEO, FLASHCARD, ASSIGNMENT.
 */
@Data
public class CreateChapterItemRequest {

    @NotBlank(message = "itemType không được để trống")
    private String itemType;

    /** ID tham chiếu đến bảng thật (quiz.id, video.id...). Có thể null với NOTE */
    private UUID refId;

    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    /** Metadata bổ sung (nội dung note, cấu hình quiz...) */
    private Map<String, Object> metadata;
}
