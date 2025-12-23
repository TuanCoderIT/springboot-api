package com.example.springboot_api.dto.lecturer.chapter;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class ReorderChapterItemRequest {
    @NotEmpty(message = "Danh sách ID không được để trống")
    private List<UUID> orderedIds;
}
