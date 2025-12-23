package com.example.springboot_api.dto.lecturer.chapter;

import java.util.Map;

import lombok.Data;

/**
 * DTO để cập nhật ChapterItem.
 */
@Data
public class UpdateChapterItemRequest {
    private String title;
    private Map<String, Object> metadata;
}
