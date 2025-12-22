package com.example.springboot_api.mappers;

import org.springframework.stereotype.Component;

import com.example.springboot_api.dto.admin.assignment.AssignmentResponse;
import com.example.springboot_api.models.TeachingAssignment;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AssignmentMapper {

    private final TermMapper termMapper;
    private final SubjectMapper subjectMapper;
    private final LecturerMapper lecturerMapper;

    public AssignmentResponse toAssignmentResponse(TeachingAssignment ta, Long classCount, Long studentCount) {
        if (ta == null)
            return null;

        return AssignmentResponse.builder()
                .id(ta.getId())
                .term(termMapper.toTermResponse(ta.getTerm(), null))
                .subject(subjectMapper.toSubjectResponse(ta.getSubject(), null, null, null))
                .teacher(lecturerMapper.toLecturerResponse(ta.getLecturer()))
                .status(ta.getStatus())
                .approvalStatus(ta.getApprovalStatus())
                .createdBy(ta.getCreatedBy())
                .approvedBy(ta.getApprovedBy())
                .approvedAt(ta.getApprovedAt())
                .createdAt(ta.getCreatedAt())
                .note(ta.getNote())
                .classCount(classCount != null ? classCount : 0L)
                .studentCount(studentCount != null ? studentCount : 0L)
                .build();
    }
}
