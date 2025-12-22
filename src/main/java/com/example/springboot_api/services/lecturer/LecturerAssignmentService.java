package com.example.springboot_api.services.lecturer;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot_api.common.exceptions.BadRequestException;
import com.example.springboot_api.common.exceptions.ForbiddenException;
import com.example.springboot_api.common.exceptions.NotFoundException;
import com.example.springboot_api.common.security.CurrentUserProvider;
import com.example.springboot_api.dto.lecturer.ClassResponse;
import com.example.springboot_api.dto.lecturer.ClassStudentResponse;
import com.example.springboot_api.dto.lecturer.LecturerAssignmentResponse;
import com.example.springboot_api.dto.lecturer.RequestTeachingRequest;
import com.example.springboot_api.dto.shared.PagedResponse;
import com.example.springboot_api.mappers.LecturerMapper;
import com.example.springboot_api.models.Class;
import com.example.springboot_api.models.ClassMember;
import com.example.springboot_api.models.Subject;
import com.example.springboot_api.models.TeachingAssignment;
import com.example.springboot_api.models.Term;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.admin.SubjectRepository;
import com.example.springboot_api.repositories.admin.TeachingAssignmentRepository;
import com.example.springboot_api.repositories.admin.TermRepository;
import com.example.springboot_api.repositories.admin.UserRepository;
import com.example.springboot_api.repositories.lecturer.ClassMemberRepository;
import com.example.springboot_api.repositories.lecturer.ClassRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service xử lý các nghiệp vụ liên quan đến phân công giảng dạy cho Giảng viên.
 */
@Service
@RequiredArgsConstructor
public class LecturerAssignmentService {

    private final TeachingAssignmentRepository assignmentRepo;
    private final TermRepository termRepo;
    private final SubjectRepository subjectRepo;
    private final UserRepository userRepo;
    private final ClassRepository classRepo;
    private final ClassMemberRepository classMemberRepo;
    private final CurrentUserProvider userProvider;
    private final LecturerMapper lecturerMapper;

    /**
     * Lấy danh sách phân công giảng dạy của giảng viên hiện tại (có phân trang và
     * lọc).
     */
    @Transactional(readOnly = true)
    public PagedResponse<LecturerAssignmentResponse> getMyAssignments(UUID termId, String status, String termStatus,
            Pageable pageable) {
        UUID lecturerId = userProvider.getCurrentUserId();
        if (lecturerId == null) {
            throw new NotFoundException("Không xác định được danh tính giảng viên");
        }

        Page<TeachingAssignment> page = assignmentRepo.findAllByLecturerWithFilters(
                lecturerId, termId, status, pageable);

        List<LecturerAssignmentResponse> items = page.getContent().stream()
                .map(ta -> {
                    Long classCount = assignmentRepo.countClassesByAssignmentId(ta.getId());
                    Long studentCount = assignmentRepo.countStudentsByAssignmentId(ta.getId());
                    return lecturerMapper.toLecturerAssignmentResponse(ta, classCount, studentCount);
                })
                .filter(item -> matchesTermStatus(item.getTermStatus(), termStatus))
                .collect(Collectors.toList());

        return new PagedResponse<>(
                items,
                new PagedResponse.Meta(
                        page.getNumber(),
                        page.getSize(),
                        page.getTotalElements(),
                        page.getTotalPages()));
    }

    /**
     * Giảng viên gửi yêu cầu xin dạy môn học.
     */
    @Transactional
    public LecturerAssignmentResponse requestTeaching(RequestTeachingRequest req) {
        UUID lecturerId = userProvider.getCurrentUserId();
        if (lecturerId == null) {
            throw new NotFoundException("Không xác định được danh tính giảng viên");
        }

        // Kiểm tra trùng lặp
        boolean exists = assignmentRepo.existsByTermIdAndSubjectIdAndLecturerId(
                req.getTermId(), req.getSubjectId(), lecturerId);
        if (exists) {
            throw new BadRequestException("Bạn đã đăng ký dạy môn này trong học kỳ này rồi");
        }

        // Lấy thông tin
        Term term = termRepo.findById(req.getTermId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy học kỳ"));
        Subject subject = subjectRepo.findById(req.getSubjectId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy môn học"));
        User lecturer = userRepo.findById(lecturerId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin giảng viên"));

        OffsetDateTime now = OffsetDateTime.now();

        // Tạo yêu cầu với approvalStatus = PENDING
        TeachingAssignment ta = TeachingAssignment.builder()
                .term(term)
                .subject(subject)
                .lecturer(lecturer)
                .status("ACTIVE")
                .approvalStatus("PENDING")
                .createdBy("LECTURER")
                .createdAt(now)
                .note(req.getNote())
                .build();

        TeachingAssignment saved = assignmentRepo.save(ta);
        return lecturerMapper.toLecturerAssignmentResponse(saved, 0L, 0L);
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

        // Tạo pageable với sort
        String sortField = (sortBy != null && !sortBy.isBlank()) ? sortBy : "studentCode";
        Sort sort = "desc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortField).descending()
                : Sort.by(sortField).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ClassMember> result = classMemberRepo.findByClassIdWithFilters(classId, q, pageable);

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
     * Lấy toàn bộ sinh viên trong 1 phân công giảng dạy (từ tất cả các lớp).
     */
    @Transactional(readOnly = true)
    public PagedResponse<ClassStudentResponse> getAssignmentStudents(
            UUID assignmentId, String q, int page, int size, String sortBy, String sortDir) {
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

        // Tạo pageable với sort
        String sortField = (sortBy != null && !sortBy.isBlank()) ? sortBy : "studentCode";
        Sort sort = "desc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortField).descending()
                : Sort.by(sortField).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ClassMember> result = classMemberRepo.findByAssignmentIdWithFilters(assignmentId, q, pageable);

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
     * Kiểm tra xem item có khớp với termStatus filter không.
     */
    private boolean matchesTermStatus(String itemTermStatus, String filterTermStatus) {
        if (filterTermStatus == null || filterTermStatus.isBlank()) {
            return true; // Không filter thì cho pass
        }
        return filterTermStatus.equalsIgnoreCase(itemTermStatus);
    }
}
