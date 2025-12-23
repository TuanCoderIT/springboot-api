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
import com.example.springboot_api.dto.lecturer.ClassDetailResponse;
import com.example.springboot_api.dto.lecturer.ClassResponse;
import com.example.springboot_api.dto.shared.PagedResponse;
import com.example.springboot_api.mappers.LecturerMapper;
import com.example.springboot_api.models.Class;
import com.example.springboot_api.models.TeachingAssignment;
import com.example.springboot_api.repositories.admin.TeachingAssignmentRepository;
import com.example.springboot_api.repositories.lecturer.ClassMemberRepository;
import com.example.springboot_api.repositories.lecturer.ClassRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service xử lý các nghiệp vụ liên quan đến Lớp học phần và Sinh viên cho Giảng
 * viên.
 */
@Service
@RequiredArgsConstructor
public class LecturerClassService {

    private final ClassRepository classRepo;
    private final ClassMemberRepository classMemberRepo;
    private final TeachingAssignmentRepository assignmentRepo;
    private final CurrentUserProvider userProvider;
    private final LecturerMapper lecturerMapper;

    // Repositories để count tài liệu (từ Assignment)
    private final com.example.springboot_api.repositories.shared.NotebookFileRepository fileRepo;
    private final com.example.springboot_api.repositories.shared.QuizRepository quizRepo;
    private final com.example.springboot_api.repositories.shared.FlashcardRepository flashcardRepo;
    private final com.example.springboot_api.repositories.shared.NotebookAiSummaryRepository summaryRepo;
    // private final
    // com.example.springboot_api.repositories.shared.VideoAssetRepository
    // videoRepo;

    /**
     * Lấy danh sách tất cả lớp học phần của giảng viên (có filter học kỳ, phân
     * công,
     * tìm kiếm).
     */
    @Transactional(readOnly = true)
    public PagedResponse<ClassResponse> getAllClasses(
            UUID termId, UUID assignmentId, String q, int page, int size, String sortBy, String sortDir) {
        UUID lecturerId = userProvider.getCurrentUserId();
        if (lecturerId == null) {
            throw new NotFoundException("Không xác định được danh tính giảng viên");
        }

        // Tạo pageable với sort
        String sortField = (sortBy != null && !sortBy.isBlank()) ? sortBy : "classCode";
        Sort sort = "desc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortField).descending()
                : Sort.by(sortField).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Class> result = classRepo.findAllByLecturerWithFilters(
                lecturerId, termId, assignmentId, q, pageable);

        List<ClassResponse> content = result.getContent().stream()
                .map(c -> {
                    // Đếm số sinh viên cho mỗi lớp
                    Long studentCount = classMemberRepo.countByClassId(c.getId());
                    return lecturerMapper.toClassResponse(c, studentCount);
                })
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
     * Lấy danh sách lớp học phần của một phân công giảng dạy (có phân trang, tìm
     * kiếm, sắp xếp).
     */
    @Transactional(readOnly = true)
    public PagedResponse<ClassResponse> getMyClasses(
            UUID assignmentId, String q, int page, int size, String sortBy, String sortDir) {
        UUID lecturerId = userProvider.getCurrentUserId();
        if (lecturerId == null) {
            throw new NotFoundException("Không xác định được danh tính giảng viên");
        }

        // Kiểm tra assignment thuộc về giảng viên hiện tại
        TeachingAssignment ta = assignmentRepo.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phân công giảng dạy"));

        if (!ta.getLecturer().getId().equals(lecturerId)) {
            throw new ForbiddenException("Bạn không có quyền xem lớp của phân công này");
        }

        // Tạo pageable với sort
        String sortField = (sortBy != null && !sortBy.isBlank()) ? sortBy : "classCode";
        Sort sort = "desc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortField).descending()
                : Sort.by(sortField).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Class> result = classRepo.findByAssignmentIdWithFilters(assignmentId, q, pageable);

        List<ClassResponse> content = result.getContent().stream()
                .map(c -> {
                    Long studentCount = classMemberRepo.countByClassId(c.getId());
                    return lecturerMapper.toClassResponse(c, studentCount);
                })
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
     * Lấy chi tiết lớp học phần theo ID.
     */
    @Transactional(readOnly = true)
    public ClassDetailResponse getClassDetail(UUID classId) {
        UUID lecturerId = userProvider.getCurrentUserId();
        if (lecturerId == null) {
            throw new NotFoundException("Không xác định được danh tính giảng viên");
        }

        Class classEntity = classRepo.findById(classId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lớp học phần"));

        TeachingAssignment ta = classEntity.getTeachingAssignment();
        if (ta == null || !ta.getLecturer().getId().equals(lecturerId)) {
            throw new ForbiddenException("Bạn không có quyền xem thông tin lớp này");
        }

        Long studentCount = classMemberRepo.countByClassId(classId);

        // Count resources from Assignment's Notebook
        Long fileCount = 0L;
        Long quizCount = 0L;
        Long flashcardCount = 0L;
        Long summaryCount = 0L;
        Long videoCount = 0L;

        if (ta.getNotebook() != null) {
            UUID notebookId = ta.getNotebook().getId();
            fileCount = fileRepo.countByNotebookId(notebookId);
            quizCount = quizRepo.countByNotebookId(notebookId);
            flashcardCount = flashcardRepo.countByNotebookId(notebookId);
            summaryCount = summaryRepo.countByNotebookId(notebookId);
        }

        return lecturerMapper.toClassDetailResponse(classEntity, studentCount,
                fileCount, quizCount, flashcardCount, summaryCount, videoCount);
    }
}
