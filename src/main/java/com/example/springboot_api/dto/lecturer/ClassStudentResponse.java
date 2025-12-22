package com.example.springboot_api.dto.lecturer;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response cho thông tin sinh viên trong lớp học phần.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassStudentResponse {
    private UUID id;
    private String studentCode;
    private String fullName;
    private String firstName;
    private String lastName;
    private LocalDate dob;

    // Thông tin lớp học
    private String classCode;
    private String subjectCode;
    private String subjectName;
    private String termName;

    private OffsetDateTime createdAt;
}
