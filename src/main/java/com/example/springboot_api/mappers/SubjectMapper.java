package com.example.springboot_api.mappers;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.example.springboot_api.dto.admin.subject.AssignmentInfo;
import com.example.springboot_api.dto.admin.subject.ClassInfo;
import com.example.springboot_api.dto.admin.subject.MajorInSubjectInfo;
import com.example.springboot_api.dto.admin.subject.SubjectDetailResponse;
import com.example.springboot_api.dto.admin.subject.SubjectResponse;
import com.example.springboot_api.models.Subject;

/**
 * Mapper chuyển đổi Subject entity sang DTOs.
 */
@Component
public class SubjectMapper {

    /**
     * Convert Subject entity sang SubjectResponse DTO.
     */
    public SubjectResponse toSubjectResponse(Subject subject, Long majorCount, Long assignmentCount,
            Long studentCount) {
        if (subject == null) {
            return null;
        }

        return SubjectResponse.builder()
                .id(subject.getId())
                .code(subject.getCode())
                .name(subject.getName())
                .credit(subject.getCredit())
                .isActive(subject.getIsActive())
                .createdAt(subject.getCreatedAt())
                .updatedAt(subject.getUpdatedAt())
                .majorCount(majorCount != null ? majorCount : 0L)
                .assignmentCount(assignmentCount != null ? assignmentCount : 0L)
                .studentCount(studentCount != null ? studentCount : 0L)
                .build();
    }

    /**
     * Convert Subject entity sang SubjectDetailResponse DTO.
     */
    public SubjectDetailResponse toSubjectDetailResponse(
            Subject subject,
            Long majorCount,
            Long assignmentCount,
            Long studentCount,
            List<MajorInSubjectInfo> majors,
            List<AssignmentInfo> assignments) {
        if (subject == null) {
            return null;
        }

        return SubjectDetailResponse.builder()
                .id(subject.getId())
                .code(subject.getCode())
                .name(subject.getName())
                .credit(subject.getCredit())
                .isActive(subject.getIsActive())
                .createdAt(subject.getCreatedAt())
                .updatedAt(subject.getUpdatedAt())
                .majorCount(majorCount != null ? majorCount : 0L)
                .assignmentCount(assignmentCount != null ? assignmentCount : 0L)
                .studentCount(studentCount != null ? studentCount : 0L)
                .majors(majors)
                .assignments(assignments)
                .build();
    }

    /**
     * Convert Object[] row từ query sang MajorInSubjectInfo.
     */
    public MajorInSubjectInfo toMajorInSubjectInfo(Object[] row) {
        if (row == null) {
            return null;
        }

        return MajorInSubjectInfo.builder()
                .id((UUID) row[0])
                .code((String) row[1])
                .name((String) row[2])
                .termNo((Integer) row[3])
                .isRequired(safeToBoolean(row[4]))
                .knowledgeBlock((String) row[5])
                .build();
    }

    private Boolean safeToBoolean(Object obj) {
        if (obj == null)
            return null;
        if (obj instanceof Boolean)
            return (Boolean) obj;
        if (obj instanceof Number)
            return ((Number) obj).intValue() != 0;
        return Boolean.parseBoolean(obj.toString());
    }

    /**
     * Convert list Object[] rows sang list MajorInSubjectInfo.
     */
    public List<MajorInSubjectInfo> toMajorInSubjectInfoList(List<Object[]> rows) {
        if (rows == null) {
            return List.of();
        }

        return rows.stream()
                .map(this::toMajorInSubjectInfo)
                .collect(Collectors.toList());
    }

    /**
     * Mapper cho AssignmentInfo từ query rows (Nâng cao)
     * Row format: [assignmentId, termName, lecturerFullName, status, createdAt,
     * approvalStatus, note, lecturerEmail]
     */
    public AssignmentInfo toAssignmentInfo(Object[] row, Long classCount, List<ClassInfo> classes) {
        if (row == null)
            return null;
        return AssignmentInfo.builder()
                .id((UUID) row[0])
                .termName((String) row[1])
                .lecturerName((String) row[2])
                .status((String) row[3])
                .createdAt((OffsetDateTime) row[4])
                .approvalStatus((String) row[5])
                .note((String) row[6])
                .lecturerEmail((String) row[7])
                .classCount(classCount != null ? classCount : 0L)
                .classes(classes)
                .build();
    }

    /**
     * Mapper cho ClassInfo từ Class entity
     */
    public ClassInfo toClassInfo(com.example.springboot_api.models.Class clazz) {
        if (clazz == null)
            return null;
        return ClassInfo.builder()
                .id(clazz.getId())
                .code(clazz.getClassCode())
                .name(clazz.getSubjectName())
                .note(clazz.getNote())
                .isActive(clazz.getIsActive())
                .build();
    }
}
