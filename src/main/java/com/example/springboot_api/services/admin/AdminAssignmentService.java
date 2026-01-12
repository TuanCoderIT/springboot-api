package com.example.springboot_api.services.admin;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot_api.common.exceptions.NotFoundException;
import com.example.springboot_api.common.security.CurrentUserProvider;
import com.example.springboot_api.dto.admin.assignment.ApproveAssignmentRequest;
import com.example.springboot_api.dto.admin.assignment.AssignmentResponse;
import com.example.springboot_api.dto.admin.assignment.CreateAssignmentRequest;
import com.example.springboot_api.mappers.AssignmentMapper;
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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminAssignmentService {

    private final TeachingAssignmentRepository assignmentRepo;
    private final TermRepository termRepo;
    private final SubjectRepository subjectRepo;
    private final UserRepository userRepo;
    private final NotebookRepository notebookRepo;
    private final NotebookMemberRepository notebookMemberRepo;
    private final AssignmentMapper assignmentMapper;
    private final CurrentUserProvider userProvider;

    @Transactional(readOnly = true)
    public List<AssignmentResponse> listAssignments(UUID termId, UUID teacherId, String status) {
        return assignmentRepo.findAllWithFilters(termId, teacherId, status).stream()
                .map(ta -> {
                    Long classCount = assignmentRepo.countClassesByAssignmentId(ta.getId());
                    Long studentCount = assignmentRepo.countStudentsByAssignmentId(ta.getId());
                    return assignmentMapper.toAssignmentResponse(ta, classCount, studentCount);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public AssignmentResponse createAssignment(CreateAssignmentRequest req) {
        Term term = termRepo.findById(req.getTermId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy học kỳ"));
        Subject subject = subjectRepo.findById(req.getSubjectId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy môn học"));
        User teacher = userRepo.findById(req.getTeacherUserId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giảng viên"));

        OffsetDateTime now = OffsetDateTime.now();

        // Tạo Notebook cho assignment (vì mặc định là APPROVED)
        Notebook notebook = createNotebookWithOwner(subject, term, teacher, now);

        TeachingAssignment ta = TeachingAssignment.builder()
                .term(term)
                .subject(subject)
                .lecturer(teacher)
                .notebook(notebook)
                .status("ACTIVE")
                .approvalStatus("APPROVED")
                .createdBy("ADMIN")
                .approvedBy(userProvider.getCurrentUserId())
                .approvedAt(now)
                .createdAt(now)
                .note(req.getNote())
                .build();

        TeachingAssignment saved = assignmentRepo.save(ta);
        return assignmentMapper.toAssignmentResponse(saved, 0L, 0L);
    }

    @Transactional
    public AssignmentResponse approveOrReject(UUID id, ApproveAssignmentRequest req) {
        TeachingAssignment ta = assignmentRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phân công"));

        ta.setApprovalStatus(req.getStatus());
        ta.setNote(req.getNote());
        User approver = userRepo.findById(userProvider.getCurrentUserId())
                .orElse(null);
        ta.setApprovedBy(approver != null ? approver.getId() : null);
        ta.setApprovedAt(OffsetDateTime.now());

        // Tự động tạo Notebook khi approve (nếu chưa có)
        if ("APPROVED".equals(req.getStatus()) && ta.getNotebook() == null) {
            OffsetDateTime now = OffsetDateTime.now();
            Notebook notebook = createNotebookWithOwner(
                    ta.getSubject(), ta.getTerm(), ta.getLecturer(), now);
            ta.setNotebook(notebook);
        }

        TeachingAssignment saved = assignmentRepo.save(ta);
        Long classCount = assignmentRepo.countClassesByAssignmentId(id);
        Long studentCount = assignmentRepo.countStudentsByAssignmentId(id);
        return assignmentMapper.toAssignmentResponse(saved, classCount, studentCount);
    }

    /**
     * Tạo Notebook mới kèm NotebookMember (owner) cho giảng viên.
     */
    private Notebook createNotebookWithOwner(Subject subject, Term term, User teacher, OffsetDateTime now) {
        String notebookTitle = subject.getName() + " - " + term.getName();
        
        Notebook notebook = Notebook.builder()
                .title(notebookTitle)
                .description("Tài liệu giảng dạy cho môn " + subject.getName())
                .type("assignment")
                .visibility("private")
                .createdBy(teacher)
                .createdAt(now)
                .updatedAt(now)
                .build();
        notebook = notebookRepo.save(notebook);

        // Thêm giảng viên làm owner của notebook
        NotebookMember lecturerMember = NotebookMember.builder()
                .notebook(notebook)
                .user(teacher)
                .role("owner")
                .status("approved")
                .joinedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
        notebookMemberRepo.save(lecturerMember);

        return notebook;
    }

    @Transactional
    public void deleteAssignment(UUID id) {
        if (!assignmentRepo.existsById(id)) {
            throw new NotFoundException("Không tìm thấy phân công");
        }
        assignmentRepo.deleteById(id);
    }
}
