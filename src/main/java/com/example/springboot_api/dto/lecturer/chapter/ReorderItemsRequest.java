package com.example.springboot_api.dto.lecturer.chapter;

import java.util.List;
import java.util.UUID;

import lombok.Data;

/**
 * DTO để sắp xếp lại thứ tự items trong chapter.
 */
@Data
public class ReorderItemsRequest {
    /** Danh sách item IDs theo thứ tự mới */
    private List<UUID> orderedIds;
}
