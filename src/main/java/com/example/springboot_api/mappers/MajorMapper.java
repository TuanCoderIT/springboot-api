package com.example.springboot_api.mappers;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.example.springboot_api.dto.admin.lecturer.OrgUnitInfo;
import com.example.springboot_api.dto.admin.major.MajorDetailResponse;
import com.example.springboot_api.dto.admin.major.MajorResponse;
import com.example.springboot_api.dto.admin.major.SubjectInMajorInfo;
import com.example.springboot_api.models.Major;
import com.example.springboot_api.models.OrgUnit;

import lombok.RequiredArgsConstructor;

/**
 * Mapper chuyển đổi Major entity sang DTOs.
 */
@Component
@RequiredArgsConstructor
public class MajorMapper {

    /**
     * Convert Major entity sang MajorResponse DTO.
     */
    public MajorResponse toMajorResponse(Major major, Long subjectCount, Long studentCount) {
        if (major == null) {
            return null;
        }

        return MajorResponse.builder()
                .id(major.getId())
                .code(major.getCode())
                .name(major.getName())
                .orgUnit(toOrgUnitInfo(major.getOrgUnit()))
                .isActive(major.getIsActive())
                .createdAt(major.getCreatedAt())
                .updatedAt(major.getUpdatedAt())
                .subjectCount(subjectCount != null ? subjectCount : 0L)
                .studentCount(studentCount != null ? studentCount : 0L)
                .build();
    }

    /**
     * Convert Major entity sang MajorDetailResponse DTO.
     */
    public MajorDetailResponse toMajorDetailResponse(Major major, Long subjectCount, Long studentCount,
            List<SubjectInMajorInfo> subjects) {
        if (major == null) {
            return null;
        }

        return MajorDetailResponse.builder()
                .id(major.getId())
                .code(major.getCode())
                .name(major.getName())
                .orgUnit(toOrgUnitInfo(major.getOrgUnit()))
                .isActive(major.getIsActive())
                .createdAt(major.getCreatedAt())
                .updatedAt(major.getUpdatedAt())
                .subjectCount(subjectCount != null ? subjectCount : 0L)
                .studentCount(studentCount != null ? studentCount : 0L)
                .subjects(subjects)
                .build();
    }

    /**
     * Convert Object[] row từ query sang SubjectInMajorInfo.
     * Row format: [subjectId, subjectCode, subjectName, termNo, isRequired,
     * knowledgeBlock]
     */
    public SubjectInMajorInfo toSubjectInMajorInfo(Object[] row) {
        if (row == null) {
            return null;
        }

        return SubjectInMajorInfo.builder()
                .id((UUID) row[0])
                .code((String) row[1])
                .name((String) row[2])
                .credit((Integer) row[3])
                .termNo((Integer) row[4])
                .isRequired(safeToBoolean(row[5]))
                .knowledgeBlock((String) row[6])
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
     * Convert list Object[] rows sang list SubjectInMajorInfo.
     */
    public List<SubjectInMajorInfo> toSubjectInMajorInfoList(List<Object[]> rows) {
        if (rows == null) {
            return List.of();
        }

        return rows.stream()
                .map(this::toSubjectInMajorInfo)
                .collect(Collectors.toList());
    }

    /**
     * Convert OrgUnit entity sang OrgUnitInfo DTO.
     */
    public OrgUnitInfo toOrgUnitInfo(OrgUnit orgUnit) {
        if (orgUnit == null) {
            return null;
        }

        return OrgUnitInfo.builder()
                .id(orgUnit.getId())
                .code(orgUnit.getCode())
                .name(orgUnit.getName())
                .type(orgUnit.getType())
                .build();
    }
}
