package com.example.springboot_api.services.admin;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot_api.common.exceptions.ConflictException;
import com.example.springboot_api.common.exceptions.NotFoundException;
import com.example.springboot_api.dto.admin.subject.AssignmentInfo;
import com.example.springboot_api.dto.admin.subject.ClassInfo;
import com.example.springboot_api.dto.admin.subject.CreateSubjectRequest;
import com.example.springboot_api.dto.admin.subject.ListSubjectRequest;
import com.example.springboot_api.dto.admin.subject.MajorAssignment;
import com.example.springboot_api.dto.admin.subject.MajorInSubjectInfo;
import com.example.springboot_api.dto.admin.subject.SubjectDetailResponse;
import com.example.springboot_api.dto.admin.subject.SubjectResponse;
import com.example.springboot_api.dto.admin.subject.UpdateSubjectRequest;
import com.example.springboot_api.dto.shared.PagedResponse;
import com.example.springboot_api.mappers.SubjectMapper;
import com.example.springboot_api.models.Major;
import com.example.springboot_api.models.MajorSubject;
import com.example.springboot_api.models.Subject;
import com.example.springboot_api.repositories.admin.MajorRepository;
import com.example.springboot_api.repositories.admin.MajorSubjectRepository;
import com.example.springboot_api.repositories.admin.SubjectRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service quản lý Subject cho Admin.
 */
@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepo;
    private final MajorRepository majorRepo;
    private final MajorSubjectRepository majorSubjectRepo;
    private final SubjectMapper subjectMapper;

    /**
     * Lấy danh sách Subject với phân trang và filter
     */
    @Transactional(readOnly = true)
    public PagedResponse<SubjectResponse> list(ListSubjectRequest req) {
        String sortBy = Optional.ofNullable(req.getSortBy()).orElse("code");
        String sortDir = Optional.ofNullable(req.getSortDir()).orElse("asc");

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(req.getPage(), req.getSize(), sort);

        Page<Subject> result = subjectRepo.findAllSubjects(
                req.getQ(),
                req.getIsActive(),
                req.getMajorId(),
                pageable);

        // Map với majorCount, assignmentCount và studentCount cho mỗi subject
        List<SubjectResponse> content = result.getContent().stream()
                .map(subject -> {
                    Long majorCount = subjectRepo.countMajorsBySubjectId(subject.getId());
                    Long assignmentCount = subjectRepo.countAssignmentsBySubjectId(subject.getId());
                    Long studentCount = subjectRepo.countStudentsBySubjectId(subject.getId());
                    return subjectMapper.toSubjectResponse(subject, majorCount, assignmentCount, studentCount);
                })
                .toList();

        return new PagedResponse<>(
                content,
                new PagedResponse.Meta(
                        result.getNumber(),
                        result.getSize(),
                        result.getTotalElements(),
                        result.getTotalPages()));
    }

    /**
     * Lấy thông tin cơ bản Subject theo ID
     */
    @Transactional(readOnly = true)
    public SubjectResponse getOne(UUID id) {
        Subject subject = subjectRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy môn học"));

        Long majorCount = subjectRepo.countMajorsBySubjectId(id);
        Long assignmentCount = subjectRepo.countAssignmentsBySubjectId(id);
        Long studentCount = subjectRepo.countStudentsBySubjectId(id);
        return subjectMapper.toSubjectResponse(subject, majorCount, assignmentCount, studentCount);
    }

    /**
     * Lấy chi tiết Subject với danh sách ngành học
     */
    @Transactional(readOnly = true)
    public SubjectDetailResponse getDetail(UUID id) {
        Subject subject = subjectRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy môn học"));

        Long majorCount = subjectRepo.countMajorsBySubjectId(id);
        Long assignmentCount = subjectRepo.countAssignmentsBySubjectId(id);
        Long studentCount = subjectRepo.countStudentsBySubjectId(id);

        // Lấy danh sách ngành
        List<Object[]> majorRows = subjectRepo.findMajorsOfSubject(id);
        List<MajorInSubjectInfo> majors = subjectMapper.toMajorInSubjectInfoList(majorRows);

        // Lấy danh sách đợt giảng dạy (Nâng cao)
        List<Object[]> assignmentRows = subjectRepo.findAssignmentsBySubjectId(id);
        List<AssignmentInfo> assignments = assignmentRows.stream()
                .map(row -> {
                    UUID assignmentId = (UUID) row[0];
                    Long classCount = subjectRepo.countClassesByAssignmentId(assignmentId);

                    // Lấy chi tiết các lớp học trong đợt này
                    List<com.example.springboot_api.models.Class> classEntities = subjectRepo
                            .findClassesByAssignmentId(assignmentId);
                    List<ClassInfo> classes = classEntities.stream()
                            .map(subjectMapper::toClassInfo)
                            .toList();

                    return subjectMapper.toAssignmentInfo(row, classCount, classes);
                })
                .toList();

        return subjectMapper.toSubjectDetailResponse(subject, majorCount, assignmentCount, studentCount, majors,
                assignments);
    }

    /**
     * Tạo Subject mới
     */
    @Transactional
    public SubjectResponse create(CreateSubjectRequest req) {
        // Validate code unique
        if (subjectRepo.existsByCode(req.getCode())) {
            throw new ConflictException("Mã môn học đã tồn tại trong hệ thống");
        }

        OffsetDateTime now = OffsetDateTime.now();
        Subject subject = Subject.builder()
                .code(req.getCode())
                .name(req.getName())
                .credit(req.getCredit())
                .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        subjectRepo.save(subject);

        // Tạo MajorSubject nếu có majorAssignments
        if (req.getMajorAssignments() != null && !req.getMajorAssignments().isEmpty()) {
            saveMajorAssignments(subject, req.getMajorAssignments(), now);
        }

        Long majorCount = subjectRepo.countMajorsBySubjectId(subject.getId());
        return subjectMapper.toSubjectResponse(subject, majorCount, 0L, 0L);
    }

    /**
     * Cập nhật Subject
     */
    @Transactional
    public SubjectResponse update(UUID id, UpdateSubjectRequest req) {
        Subject subject = subjectRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy môn học"));

        // Validate code unique nếu thay đổi
        if (req.getCode() != null && !req.getCode().equals(subject.getCode())) {
            if (subjectRepo.existsByCode(req.getCode())) {
                throw new ConflictException("Mã môn học đã tồn tại trong hệ thống");
            }
            subject.setCode(req.getCode());
        }

        if (req.getName() != null) {
            subject.setName(req.getName());
        }

        if (req.getCredit() != null) {
            subject.setCredit(req.getCredit());
        }

        if (req.getIsActive() != null) {
            subject.setIsActive(req.getIsActive());
        }

        OffsetDateTime now = OffsetDateTime.now();
        subject.setUpdatedAt(now);
        subjectRepo.save(subject);

        // Cập nhật MajorSubject nếu có majorAssignments (replace toàn bộ)
        if (req.getMajorAssignments() != null) {
            majorSubjectRepo.deleteBySubjectId(id);
            if (!req.getMajorAssignments().isEmpty()) {
                saveMajorAssignments(subject, req.getMajorAssignments(), now);
            }
        }

        Long majorCount = subjectRepo.countMajorsBySubjectId(id);
        Long assignmentCount = subjectRepo.countAssignmentsBySubjectId(id);
        Long studentCount = subjectRepo.countStudentsBySubjectId(id);
        return subjectMapper.toSubjectResponse(subject, majorCount, assignmentCount, studentCount);
    }

    /**
     * Xóa Subject
     */
    @Transactional
    public void delete(UUID id) {
        Subject subject = subjectRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy môn học"));

        // Kiểm tra subject có TeachingAssignment không
        if (subjectRepo.hasAssignments(id)) {
            throw new ConflictException("Không thể xóa môn học đang có phân công giảng dạy");
        }

        // Xóa MajorSubject liên kết trước
        majorSubjectRepo.deleteBySubjectId(id);

        subjectRepo.delete(subject);
    }

    /**
     * Helper: Tạo MajorSubject records từ majorAssignments
     */
    private void saveMajorAssignments(Subject subject, List<MajorAssignment> assignments, OffsetDateTime now) {
        for (MajorAssignment assignment : assignments) {
            Major major = majorRepo.findById(assignment.getMajorId())
                    .orElseThrow(
                            () -> new NotFoundException("Không tìm thấy ngành học với ID: " + assignment.getMajorId()));

            MajorSubject ms = MajorSubject.builder()
                    .major(major)
                    .subject(subject)
                    .termNo(assignment.getTermNo())
                    .isRequired(assignment.getIsRequired() != null ? assignment.getIsRequired() : true)
                    .knowledgeBlock(assignment.getKnowledgeBlock())
                    .createdAt(now)
                    .build();

            majorSubjectRepo.save(ms);
        }
    }
}
