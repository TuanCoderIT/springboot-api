package com.example.springboot_api.dto.admin.term;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response thông tin Term cơ bản.
 */
@Data
@Builder
public class TermResponse {
    private UUID id;
    private String code;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private Long totalAssignments;  // Tổng số TeachingAssignment trong kỳ
}
