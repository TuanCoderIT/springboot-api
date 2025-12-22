package com.example.springboot_api.dto.admin.assignment;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.springboot_api.dto.admin.lecturer.LecturerResponse;
import com.example.springboot_api.dto.admin.subject.SubjectResponse;
import com.example.springboot_api.dto.admin.term.TermResponse;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssignmentResponse {
    private UUID id;
    private TermResponse term;
    private SubjectResponse subject;
    private LecturerResponse teacher;
    private String status;
    private String approvalStatus;
    private String createdBy;
    private UUID approvedBy;
    private OffsetDateTime approvedAt;
    private OffsetDateTime createdAt;
    private String note;
    private Long classCount;
    private Long studentCount;
}
