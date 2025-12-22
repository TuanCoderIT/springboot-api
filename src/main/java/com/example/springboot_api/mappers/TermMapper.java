package com.example.springboot_api.mappers;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.example.springboot_api.dto.admin.term.SubjectInTermInfo;
import com.example.springboot_api.dto.admin.term.TermDetailResponse;
import com.example.springboot_api.dto.admin.term.TermResponse;
import com.example.springboot_api.models.Term;

/**
 * Mapper chuyển đổi Term entity sang DTOs.
 */
@Component
public class TermMapper {

    /**
     * Convert Term entity sang TermResponse DTO.
     */
    public TermResponse toTermResponse(Term term, Long totalAssignments) {
        if (term == null) {
            return null;
        }

        return TermResponse.builder()
                .id(term.getId())
                .code(term.getCode())
                .name(term.getName())
                .startDate(term.getStartDate())
                .endDate(term.getEndDate())
                .isActive(term.getIsActive())
                .createdAt(term.getCreatedAt())
                .totalAssignments(totalAssignments != null ? totalAssignments : 0L)
                .build();
    }

    /**
     * Convert Term entity sang TermDetailResponse DTO với danh sách môn học.
     */
    public TermDetailResponse toTermDetailResponse(Term term, Long totalAssignments, List<SubjectInTermInfo> subjects) {
        if (term == null) {
            return null;
        }

        return TermDetailResponse.builder()
                .id(term.getId())
                .code(term.getCode())
                .name(term.getName())
                .startDate(term.getStartDate())
                .endDate(term.getEndDate())
                .isActive(term.getIsActive())
                .createdAt(term.getCreatedAt())
                .totalAssignments(totalAssignments != null ? totalAssignments : 0L)
                .subjects(subjects)
                .build();
    }

    /**
     * Convert Object[] row từ query sang SubjectInTermInfo.
     * Row format: [subjectId, subjectCode, subjectName, credit, teacherCount]
     */
    public SubjectInTermInfo toSubjectInTermInfo(Object[] row) {
        if (row == null) {
            return null;
        }

        return SubjectInTermInfo.builder()
                .id((UUID) row[0])
                .code((String) row[1])
                .name((String) row[2])
                .credit((Integer) row[3])
                .teacherCount((Long) row[4])
                .build();
    }

    /**
     * Convert list Object[] rows sang list SubjectInTermInfo.
     */
    public List<SubjectInTermInfo> toSubjectInTermInfoList(List<Object[]> rows) {
        if (rows == null) {
            return List.of();
        }

        return rows.stream()
                .map(this::toSubjectInTermInfo)
                .collect(Collectors.toList());
    }
}
