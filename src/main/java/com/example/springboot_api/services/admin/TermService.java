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
import com.example.springboot_api.dto.admin.term.CreateTermRequest;
import com.example.springboot_api.dto.admin.term.ListTermRequest;
import com.example.springboot_api.dto.admin.term.SubjectInTermInfo;
import com.example.springboot_api.dto.admin.term.TermDetailResponse;
import com.example.springboot_api.dto.admin.term.TermResponse;
import com.example.springboot_api.dto.admin.term.UpdateTermRequest;
import com.example.springboot_api.dto.shared.PagedResponse;
import com.example.springboot_api.mappers.TermMapper;
import com.example.springboot_api.models.Term;
import com.example.springboot_api.repositories.admin.TermRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service quản lý Term cho Admin.
 */
@Service
@RequiredArgsConstructor
public class TermService {

    private final TermRepository termRepo;
    private final TermMapper termMapper;

    /**
     * Lấy danh sách Term với phân trang và filter
     */
    @Transactional(readOnly = true)
    public PagedResponse<TermResponse> list(ListTermRequest req) {
        String sortBy = Optional.ofNullable(req.getSortBy()).orElse("startDate");
        String sortDir = Optional.ofNullable(req.getSortDir()).orElse("desc");

        // Mặc định sort theo startDate DESC, sau đó endDate DESC
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending().and(Sort.by("endDate").ascending())
                : Sort.by(sortBy).descending().and(Sort.by("endDate").descending());

        Pageable pageable = PageRequest.of(req.getPage(), req.getSize(), sort);

        Page<Term> result = termRepo.findAllTerms(req.getQ(), req.getIsActive(), pageable);

        // Map với totalAssignments cho mỗi term
        List<TermResponse> content = result.getContent().stream()
                .map(term -> {
                    Long totalAssignments = termRepo.countAssignmentsByTermId(term.getId());
                    return termMapper.toTermResponse(term, totalAssignments);
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
     * Lấy danh sách học kỳ khả dụng (endDate >= ngày hiện tại)
     * Dùng cho giảng viên khi xin đăng ký dạy môn
     */
    @Transactional(readOnly = true)
    public PagedResponse<TermResponse> listAvailable(ListTermRequest req) {
        Pageable pageable = PageRequest.of(req.getPage(), req.getSize());

        Page<Term> result = termRepo.findAvailableTerms(req.getQ(), req.getIsActive(), pageable);

        List<TermResponse> content = result.getContent().stream()
                .map(term -> {
                    Long totalAssignments = termRepo.countAssignmentsByTermId(term.getId());
                    return termMapper.toTermResponse(term, totalAssignments);
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
     * Lấy thông tin cơ bản Term theo ID
     */
    @Transactional(readOnly = true)
    public TermResponse getOne(UUID id) {
        Term term = termRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy học kỳ"));

        Long totalAssignments = termRepo.countAssignmentsByTermId(id);
        return termMapper.toTermResponse(term, totalAssignments);
    }

    /**
     * Lấy chi tiết Term với danh sách môn học được mở
     */
    @Transactional(readOnly = true)
    public TermDetailResponse getDetail(UUID id) {
        Term term = termRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy học kỳ"));

        Long totalAssignments = termRepo.countAssignmentsByTermId(id);
        List<Object[]> subjectRows = termRepo.findSubjectsInTerm(id);
        List<SubjectInTermInfo> subjects = termMapper.toSubjectInTermInfoList(subjectRows);

        return termMapper.toTermDetailResponse(term, totalAssignments, subjects);
    }

    /**
     * Tạo Term mới
     */
    @Transactional
    public TermResponse create(CreateTermRequest req) {
        // Validate code unique
        if (termRepo.existsByCode(req.getCode())) {
            throw new ConflictException("Mã học kỳ đã tồn tại trong hệ thống");
        }

        Term term = Term.builder()
                .code(req.getCode())
                .name(req.getName())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                .createdAt(OffsetDateTime.now())
                .build();

        termRepo.save(term);

        return termMapper.toTermResponse(term, 0L);
    }

    /**
     * Cập nhật Term
     */
    @Transactional
    public TermResponse update(UUID id, UpdateTermRequest req) {
        Term term = termRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy học kỳ"));

        // Validate code unique nếu thay đổi
        if (req.getCode() != null && !req.getCode().equals(term.getCode())) {
            if (termRepo.existsByCode(req.getCode())) {
                throw new ConflictException("Mã học kỳ đã tồn tại trong hệ thống");
            }
            term.setCode(req.getCode());
        }

        if (req.getName() != null) {
            term.setName(req.getName());
        }

        if (req.getStartDate() != null) {
            term.setStartDate(req.getStartDate());
        }

        if (req.getEndDate() != null) {
            term.setEndDate(req.getEndDate());
        }

        if (req.getIsActive() != null) {
            term.setIsActive(req.getIsActive());
        }

        termRepo.save(term);

        Long totalAssignments = termRepo.countAssignmentsByTermId(id);
        return termMapper.toTermResponse(term, totalAssignments);
    }

    /**
     * Xóa Term
     */
    @Transactional
    public void delete(UUID id) {
        Term term = termRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy học kỳ"));

        // Kiểm tra term có TeachingAssignment không
        if (termRepo.hasAssignments(id)) {
            throw new ConflictException("Không thể xóa học kỳ đang có phân công giảng dạy");
        }

        termRepo.delete(term);
    }
}
