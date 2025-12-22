package com.example.springboot_api.dto.admin.term;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response chi tiết Term với danh sách môn học.
 */
@Data
@Builder
public class TermDetailResponse {
    private UUID id;
    private String code;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private Long totalAssignments;
    private List<SubjectInTermInfo> subjects;  // Danh sách môn học được mở trong kỳ
}
