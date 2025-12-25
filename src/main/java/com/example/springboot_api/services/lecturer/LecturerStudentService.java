package com.example.springboot_api.services.lecturer;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot_api.common.exceptions.ForbiddenException;
import com.example.springboot_api.common.exceptions.NotFoundException;
import com.example.springboot_api.common.security.CurrentUserProvider;
import com.example.springboot_api.dto.lecturer.ClassStudentResponse;
import com.example.springboot_api.dto.shared.PagedResponse;
import com.example.springboot_api.mappers.LecturerMapper;
import com.example.springboot_api.models.Class;
import com.example.springboot_api.models.ClassMember;
import com.example.springboot_api.models.TeachingAssignment;
import com.example.springboot_api.repositories.admin.TeachingAssignmentRepository;
import com.example.springboot_api.repositories.lecturer.ClassMemberRepository;
import com.example.springboot_api.repositories.lecturer.ClassRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service xử lý tìm kiếm và lấy thông tin Sinh viên dành cho Giảng viên.
 */
@Service
@RequiredArgsConstructor
public class LecturerStudentService {

    private final ClassRepository classRepo;
    private final ClassMemberRepository classMemberRepo;
    private final TeachingAssignmentRepository assignmentRepo;
    private final CurrentUserProvider userProvider;
    private final LecturerMapper lecturerMapper;

    /**
     * Lấy danh sách sinh viên trong lớp học phần (có phân trang, tìm kiếm, sắp
     * xếp).
     */
    @Transactional(readOnly = true)
    public PagedResponse<ClassStudentResponse> getClassStudents(
            UUID classId, String q, int page, int size, String sortBy, String sortDir) {
        UUID lecturerId = userProvider.getCurrentUserId();
        if (lecturerId == null) {
            throw new NotFoundException("Không xác định được danh tính giảng viên");
        }

        // Kiểm tra lớp tồn tại và thuộc về giảng viên
        Class classEntity = classRepo.findById(classId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lớp học phần"));

        TeachingAssignment ta = classEntity.getTeachingAssignment();
        if (ta == null || !ta.getLecturer().getId().equals(lecturerId)) {
            throw new ForbiddenException("Bạn không có quyền xem sinh viên của lớp này");
        }

        return getStudents(classId, null, q, page, size, sortBy, sortDir);
    }

    /**
     * Lấy sinh viên trong 1 phân công giảng dạy (có thể lọc theo lớp cụ thể).
     */
    @Transactional(readOnly = true)
    public PagedResponse<ClassStudentResponse> getAssignmentStudents(
            UUID assignmentId, UUID classId, String q, int page, int size, String sortBy, String sortDir) {
        UUID lecturerId = userProvider.getCurrentUserId();
        if (lecturerId == null) {
            throw new NotFoundException("Không xác định được danh tính giảng viên");
        }

        // Kiểm tra assignment thuộc về giảng viên
        TeachingAssignment ta = assignmentRepo.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phân công giảng dạy"));

        if (!ta.getLecturer().getId().equals(lecturerId)) {
            throw new ForbiddenException("Bạn không có quyền xem sinh viên của phân công này");
        }

        return getStudents(null, assignmentId, q, page, size, sortBy, sortDir);
    }

    // Helper method để tái sử dụng logic query
    private PagedResponse<ClassStudentResponse> getStudents(
            UUID classId, UUID assignmentId, String q, int page, int size, String sortBy, String sortDir) {

        String sortField = mapSortField(sortBy);
        Sort sort = "desc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortField).descending()
                : Sort.by(sortField).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ClassMember> result;
        if (classId != null) {
            result = classMemberRepo.findByClassIdWithFilters(classId, q, pageable);
        } else {
            result = classMemberRepo.findByAssignmentIdWithFilters(assignmentId, classId, q, pageable);
        }

        List<ClassStudentResponse> content = result.getContent().stream()
                .map(lecturerMapper::toClassStudentResponse)
                .collect(Collectors.toList());

        return new PagedResponse<>(
                content,
                new PagedResponse.Meta(
                        result.getNumber(),
                        result.getSize(),
                        result.getTotalElements(),
                        result.getTotalPages()));
    }

    /**
     * Map field name từ frontend sang field name trong ClassMember entity.
     * Đảm bảo an toàn khi sorting để tránh lỗi PropertyReferenceException.
     */
    private String mapSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "studentCode";
        }

        // Map các field thường dùng
        return switch (sortBy) {
            case "studentCode", "student_code" -> "studentCode";
            case "fullName", "full_name" -> "fullName";
            case "email" -> "email";
            case "createdAt", "created_at" -> "createdAt";
            default -> "studentCode"; // Fallback an toàn
        };
    }
}
