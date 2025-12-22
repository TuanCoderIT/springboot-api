package com.example.springboot_api.mappers;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.example.springboot_api.dto.admin.lecturer.LecturerResponse;
import com.example.springboot_api.dto.admin.lecturer.OrgUnitInfo;
import com.example.springboot_api.dto.lecturer.ClassResponse;
import com.example.springboot_api.dto.lecturer.ClassStudentResponse;
import com.example.springboot_api.dto.lecturer.LecturerAssignmentResponse;
import com.example.springboot_api.models.Class;
import com.example.springboot_api.models.ClassMember;
import com.example.springboot_api.models.OrgUnit;
import com.example.springboot_api.models.TeachingAssignment;
import com.example.springboot_api.models.Term;
import com.example.springboot_api.models.User;
import com.example.springboot_api.utils.UrlNormalizer;

import lombok.RequiredArgsConstructor;

/**
 * Mapper chuyển đổi User entities sang Lecturer DTOs.
 */
@Component
@RequiredArgsConstructor
public class LecturerMapper {

    private final UrlNormalizer urlNormalizer;

    /**
     * Convert User entity sang LecturerResponse DTO.
     */
    public LecturerResponse toLecturerResponse(User user) {
        if (user == null) {
            return null;
        }

        String avatarUrl = user.getAvatarUrl();
        if (avatarUrl != null && !avatarUrl.startsWith("http")) {
            avatarUrl = urlNormalizer.normalizeToFull(avatarUrl);
        }

        return LecturerResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .avatarUrl(avatarUrl)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lecturerCode(user.getLecturerCode())
                .academicDegree(user.getAcademicDegree())
                .academicRank(user.getAcademicRank())
                .specialization(user.getSpecialization())
                .orgUnit(toOrgUnitInfo(user.getPrimaryOrgUnit()))
                .build();
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

    /**
     * Convert TeachingAssignment entity sang LecturerAssignmentResponse DTO.
     */
    public LecturerAssignmentResponse toLecturerAssignmentResponse(TeachingAssignment ta, Long classCount,
            Long studentCount) {
        if (ta == null) {
            return null;
        }

        String termStatus = calculateTermStatus(ta.getTerm());

        return LecturerAssignmentResponse.builder()
                .id(ta.getId())
                .subjectCode(ta.getSubject() != null ? ta.getSubject().getCode() : null)
                .subjectName(ta.getSubject() != null ? ta.getSubject().getName() : null)
                .termName(ta.getTerm() != null ? ta.getTerm().getName() : null)
                .status(ta.getStatus())
                .approvalStatus(ta.getApprovalStatus())
                .classCount(classCount)
                .studentCount(studentCount)
                .createdAt(ta.getCreatedAt())
                .termStatus(termStatus)
                .build();
    }

    /**
     * Convert Class entity sang ClassResponse DTO.
     */
    public ClassResponse toClassResponse(Class classEntity, Long studentCount) {
        if (classEntity == null) {
            return null;
        }

        // Lấy tên học kỳ từ TeachingAssignment
        String termName = null;
        if (classEntity.getTeachingAssignment() != null
                && classEntity.getTeachingAssignment().getTerm() != null) {
            termName = classEntity.getTeachingAssignment().getTerm().getName();
        }

        return ClassResponse.builder()
                .id(classEntity.getId())
                .classCode(classEntity.getClassCode())
                .subjectCode(classEntity.getSubjectCode())
                .subjectName(classEntity.getSubjectName())
                .termName(termName)
                .room(classEntity.getRoom())
                .dayOfWeek(classEntity.getDayOfWeek())
                .periods(classEntity.getPeriods())
                .startDate(classEntity.getStartDate())
                .endDate(classEntity.getEndDate())
                .note(classEntity.getNote())
                .isActive(classEntity.getIsActive())
                .studentCount(studentCount)
                .createdAt(classEntity.getCreatedAt())
                .updatedAt(classEntity.getUpdatedAt())
                .build();
    }

    /**
     * Convert ClassMember entity sang ClassStudentResponse DTO.
     */
    public ClassStudentResponse toClassStudentResponse(ClassMember member) {
        if (member == null) {
            return null;
        }

        // Lấy thông tin lớp học
        String classCode = null;
        String subjectCode = null;
        String subjectName = null;
        String termName = null;

        Class classEntity = member.getClassField();
        if (classEntity != null) {
            classCode = classEntity.getClassCode();
            subjectCode = classEntity.getSubjectCode();
            subjectName = classEntity.getSubjectName();

            // Lấy tên học kỳ từ TeachingAssignment
            if (classEntity.getTeachingAssignment() != null
                    && classEntity.getTeachingAssignment().getTerm() != null) {
                termName = classEntity.getTeachingAssignment().getTerm().getName();
            }
        }

        return ClassStudentResponse.builder()
                .id(member.getId())
                .studentCode(member.getStudentCode())
                .fullName(member.getFullName())
                .firstName(member.getFirstName())
                .lastName(member.getLastName())
                .dob(member.getDob())
                .classCode(classCode)
                .subjectCode(subjectCode)
                .subjectName(subjectName)
                .termName(termName)
                .createdAt(member.getCreatedAt())
                .build();
    }

    /**
     * Tính trạng thái học kỳ dựa trên ngày hiện tại.
     * 
     * @return ACTIVE (đang diễn ra), UPCOMING (chưa đến), PAST (đã kết thúc)
     */
    private String calculateTermStatus(Term term) {
        if (term == null) {
            return null;
        }

        LocalDate today = LocalDate.now();
        LocalDate startDate = term.getStartDate();
        LocalDate endDate = term.getEndDate();

        // Nếu không có ngày bắt đầu/kết thúc, trả về null
        if (startDate == null || endDate == null) {
            return null;
        }

        if (today.isBefore(startDate)) {
            return "UPCOMING"; // Chưa đến
        } else if (today.isAfter(endDate)) {
            return "PAST"; // Đã kết thúc
        } else {
            return "ACTIVE"; // Đang diễn ra
        }
    }
}
