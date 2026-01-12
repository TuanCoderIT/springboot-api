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
import com.example.springboot_api.dto.lecturer.LecturerAssignmentDetailResponse;
import com.example.springboot_api.dto.lecturer.LecturerAssignmentResponse;
import com.example.springboot_api.dto.lecturer.RequestTeachingRequest;
import com.example.springboot_api.dto.shared.PagedResponse;
import com.example.springboot_api.mappers.LecturerMapper;
import com.example.springboot_api.models.Class;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookMember;
import com.example.springboot_api.models.Subject;
import com.example.springboot_api.models.TeachingAssignment;
import com.example.springboot_api.models.Term;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.admin.NotebookMemberRepository;
import com.example.springboot_api.repositories.admin.NotebookRepository;
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
        private final NotebookRepository notebookRepo;
        private final NotebookMemberRepository notebookMemberRepo;

        // Repositories để count tài liệu
        private final com.example.springboot_api.repositories.shared.NotebookFileRepository fileRepo;
        private final com.example.springboot_api.repositories.shared.QuizRepository quizRepo;
        private final com.example.springboot_api.repositories.shared.FlashcardRepository flashcardRepo;
        private final com.example.springboot_api.repositories.shared.NotebookAiSummaryRepository summaryRepo;
        private final com.example.springboot_api.repositories.shared.VideoAssetRepository videoRepo;

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

                                        // Count tài liệu từ Notebook
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
                                                // Video không có countByNotebookId trực tiếp, dùng 0L tạm
                                        }

                                        return lecturerMapper.toLecturerAssignmentResponse(
                                                        ta, classCount, studentCount,
                                                        fileCount, quizCount, flashcardCount, summaryCount, videoCount);
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

                // Tạo Notebook cho Teaching Assignment
                Notebook notebook = createNotebookForAssignment(subject, term, lecturer, now);

                // Tạo yêu cầu với approvalStatus = PENDING và gắn notebook
                TeachingAssignment ta = TeachingAssignment.builder()
                                .term(term)
                                .subject(subject)
                                .lecturer(lecturer)
                                .notebook(notebook)
                                .status("ACTIVE")
                                .approvalStatus("PENDING")
                                .createdBy("LECTURER")
                                .createdAt(now)
                                .note(req.getNote())
                                .build();

                TeachingAssignment saved = assignmentRepo.save(ta);
                return lecturerMapper.toLecturerAssignmentResponse(saved, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
        }

        /**
         * Tạo Notebook mới cho Teaching Assignment.
         */
        private Notebook createNotebookForAssignment(Subject subject, Term term, User lecturer, OffsetDateTime now) {
                Notebook notebook = Notebook.builder()
                                .title(subject.getName() + " - " + term.getName())
                                .description("Notebook cho môn " + subject.getName())
                                .type("assignment")
                                .visibility("private")
                                .createdBy(lecturer)
                                .createdAt(now)
                                .updatedAt(now)
                                .build();

                notebook = notebookRepo.save(notebook);

                // Thêm giảng viên làm owner của notebook
                NotebookMember lecturerMember = NotebookMember.builder()
                                .notebook(notebook)
                                .user(lecturer)
                                .role("owner")
                                .status("approved")
                                .joinedAt(now)
                                .createdAt(now)
                                .updatedAt(now)
                                .build();

                notebookMemberRepo.save(lecturerMember);

                return notebook;
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

        /**
         * Lấy chi tiết 1 phân công giảng dạy.
         */
        @Transactional(readOnly = true)
        public LecturerAssignmentDetailResponse getAssignmentDetail(UUID assignmentId) {
                UUID lecturerId = userProvider.getCurrentUserId();
                if (lecturerId == null) {
                        throw new NotFoundException("Không xác định được danh tính giảng viên");
                }

                // Lấy assignment và kiểm tra quyền
                TeachingAssignment ta = assignmentRepo.findById(assignmentId)
                                .orElseThrow(() -> new NotFoundException("Không tìm thấy phân công giảng dạy"));

                if (!ta.getLecturer().getId().equals(lecturerId)) {
                        throw new ForbiddenException("Bạn không có quyền xem phân công này");
                }

                // Count classes và students
                Long classCount = assignmentRepo.countClassesByAssignmentId(assignmentId);
                Long studentCount = assignmentRepo.countStudentsByAssignmentId(assignmentId);

                // Count tài liệu từ Notebook
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

                // Lấy top 5 lớp học phần
                Pageable pageable = PageRequest.of(0, 5, Sort.by("classCode").ascending());
                Page<Class> classPage = classRepo.findByAssignmentIdWithFilters(assignmentId, null, pageable);
                List<Class> recentClasses = classPage.getContent();

                return lecturerMapper.toAssignmentDetailResponse(
                                ta, classCount, studentCount,
                                fileCount, quizCount, flashcardCount, summaryCount, videoCount,
                                recentClasses, classMemberRepo::countByClassId);
        }
}
