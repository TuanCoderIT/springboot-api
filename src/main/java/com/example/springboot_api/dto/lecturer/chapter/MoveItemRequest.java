package com.example.springboot_api.dto.lecturer.chapter;

import java.util.UUID;

import lombok.Data;

/**
 * DTO để di chuyển item sang chapter khác hoặc đổi vị trí trong chapter.
 */
@Data
public class MoveItemRequest {
    /** ID chapter đích */
    private UUID targetChapterId;

    /** Vị trí trong chapter đích (0-based). Null = cuối danh sách */
    private Integer targetIndex;
}
