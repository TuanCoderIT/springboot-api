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
import com.example.springboot_api.dto.admin.major.CreateMajorRequest;
import com.example.springboot_api.dto.admin.major.ListMajorRequest;
import com.example.springboot_api.dto.admin.major.MajorDetailResponse;
import com.example.springboot_api.dto.admin.major.MajorResponse;
import com.example.springboot_api.dto.admin.major.SubjectInMajorInfo;
import com.example.springboot_api.dto.admin.major.UpdateMajorRequest;
import com.example.springboot_api.dto.shared.PagedResponse;
import com.example.springboot_api.mappers.MajorMapper;
import com.example.springboot_api.models.Major;
import com.example.springboot_api.models.OrgUnit;
import com.example.springboot_api.repositories.admin.MajorRepository;
import com.example.springboot_api.repositories.shared.OrgUnitRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service quản lý Major cho Admin.
 */
@Service
@RequiredArgsConstructor
public class MajorService {

    private final MajorRepository majorRepo;
    private final OrgUnitRepository orgUnitRepo;
    private final MajorMapper majorMapper;

    /**
     * Lấy danh sách Major với phân trang và filter
     */
    @Transactional(readOnly = true)
    public PagedResponse<MajorResponse> list(ListMajorRequest req) {
        String sortBy = Optional.ofNullable(req.getSortBy()).orElse("code");
        String sortDir = Optional.ofNullable(req.getSortDir()).orElse("asc");

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(req.getPage(), req.getSize(), sort);

        Page<Major> result = majorRepo.findAllMajors(
                req.getQ(),
                req.getIsActive(),
                req.getOrgUnitId(),
                pageable);

        // Map với subjectCount và studentCount cho mỗi major
        List<MajorResponse> content = result.getContent().stream()
                .map(major -> {
                    Long subjectCount = majorRepo.countSubjectsByMajorId(major.getId());
                    Long studentCount = majorRepo.countStudentsByMajorId(major.getId());
                    return majorMapper.toMajorResponse(major, subjectCount, studentCount);
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
     * Lấy thông tin cơ bản Major theo ID
     */
    @Transactional(readOnly = true)
    public MajorResponse getOne(UUID id) {
        Major major = majorRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy ngành học"));

        Long subjectCount = majorRepo.countSubjectsByMajorId(id);
        Long studentCount = majorRepo.countStudentsByMajorId(id);
        return majorMapper.toMajorResponse(major, subjectCount, studentCount);
    }

    /**
     * Lấy chi tiết Major với danh sách môn học
     */
    @Transactional(readOnly = true)
    public MajorDetailResponse getDetail(UUID id) {
        Major major = majorRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy ngành học"));

        Long subjectCount = majorRepo.countSubjectsByMajorId(id);
        Long studentCount = majorRepo.countStudentsByMajorId(id);
        List<Object[]> subjectRows = majorRepo.findSubjectsOfMajor(id);
        List<SubjectInMajorInfo> subjects = majorMapper.toSubjectInMajorInfoList(subjectRows);

        return majorMapper.toMajorDetailResponse(major, subjectCount, studentCount, subjects);
    }

    /**
     * Tạo Major mới
     */
    @Transactional
    public MajorResponse create(CreateMajorRequest req) {
        // Validate code unique
        if (majorRepo.existsByCode(req.getCode())) {
            throw new ConflictException("Mã ngành học đã tồn tại trong hệ thống");
        }

        // Lấy OrgUnit nếu có
        OrgUnit orgUnit = null;
        if (req.getOrgUnitId() != null) {
            orgUnit = orgUnitRepo.findById(req.getOrgUnitId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn vị tổ chức"));
        }

        OffsetDateTime now = OffsetDateTime.now();
        Major major = Major.builder()
                .code(req.getCode())
                .name(req.getName())
                .orgUnit(orgUnit)
                .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        majorRepo.save(major);

        return majorMapper.toMajorResponse(major, 0L, 0L);
    }

    /**
     * Cập nhật Major
     */
    @Transactional
    public MajorResponse update(UUID id, UpdateMajorRequest req) {
        Major major = majorRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy ngành học"));

        // Validate code unique nếu thay đổi
        if (req.getCode() != null && !req.getCode().equals(major.getCode())) {
            if (majorRepo.existsByCode(req.getCode())) {
                throw new ConflictException("Mã ngành học đã tồn tại trong hệ thống");
            }
            major.setCode(req.getCode());
        }

        if (req.getName() != null) {
            major.setName(req.getName());
        }

        // Cập nhật OrgUnit
        if (req.getOrgUnitId() != null) {
            OrgUnit orgUnit = orgUnitRepo.findById(req.getOrgUnitId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn vị tổ chức"));
            major.setOrgUnit(orgUnit);
        }

        if (req.getIsActive() != null) {
            major.setIsActive(req.getIsActive());
        }

        major.setUpdatedAt(OffsetDateTime.now());
        majorRepo.save(major);

        Long subjectCount = majorRepo.countSubjectsByMajorId(id);
        Long studentCount = majorRepo.countStudentsByMajorId(id);
        return majorMapper.toMajorResponse(major, subjectCount, studentCount);
    }

    /**
     * Xóa Major
     */
    @Transactional
    public void delete(UUID id) {
        Major major = majorRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy ngành học"));

        // Kiểm tra major có sinh viên không
        if (majorRepo.hasStudents(id)) {
            throw new ConflictException("Không thể xóa ngành học đang có sinh viên theo học");
        }

        // Kiểm tra major có môn học trong chương trình đào tạo không
        if (majorRepo.hasMajorSubjects(id)) {
            throw new ConflictException("Không thể xóa ngành học đang có môn học trong chương trình đào tạo");
        }

        majorRepo.delete(major);
    }
}
