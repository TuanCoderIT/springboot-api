package com.example.springboot_api.services.admin;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot_api.common.exceptions.ConflictException;
import com.example.springboot_api.common.exceptions.NotFoundException;
import com.example.springboot_api.dto.admin.lecturer.CreateLecturerRequest;
import com.example.springboot_api.dto.admin.lecturer.LecturerResponse;
import com.example.springboot_api.dto.admin.lecturer.ListLecturerRequest;
import com.example.springboot_api.dto.admin.lecturer.UpdateLecturerRequest;
import com.example.springboot_api.dto.shared.PagedResponse;
import com.example.springboot_api.mappers.LecturerMapper;
import com.example.springboot_api.models.OrgUnit;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.admin.LecturerRepository;
import com.example.springboot_api.repositories.shared.OrgUnitRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LecturerService {

    private static final String ROLE_LECTURER = "TEACHER";

    private final LecturerRepository lecturerRepo;
    private final OrgUnitRepository orgUnitRepo;
    private final BCryptPasswordEncoder encoder;
    private final LecturerMapper lecturerMapper;

    /**
     * Lấy danh sách giảng viên với phân trang và filter
     */
    @Transactional(readOnly = true)
    public PagedResponse<LecturerResponse> list(ListLecturerRequest req) {
        String sortBy = Optional.ofNullable(req.getSortBy()).orElse("createdAt");
        String sortDir = Optional.ofNullable(req.getSortDir()).orElse("desc");

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(req.getPage(), req.getSize(), sort);

        Page<User> result = lecturerRepo.findAllLecturers(req.getQ(), req.getOrgUnitId(), pageable);

        return new PagedResponse<>(
                result.map(lecturerMapper::toLecturerResponse).getContent(),
                new PagedResponse.Meta(
                        result.getNumber(),
                        result.getSize(),
                        result.getTotalElements(),
                        result.getTotalPages()));
    }

    /**
     * Lấy thông tin chi tiết một giảng viên theo ID
     */
    @Transactional(readOnly = true)
    public LecturerResponse getOne(UUID id) {
        return lecturerRepo.findLecturerById(id)
                .map(lecturerMapper::toLecturerResponse)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giảng viên"));
    }

    /**
     * Tạo giảng viên mới
     */
    @Transactional
    public LecturerResponse create(CreateLecturerRequest req) {
        // Kiểm tra email đã tồn tại chưa
        if (lecturerRepo.findByEmail(req.getEmail()).isPresent()) {
            throw new ConflictException("Email đã tồn tại trong hệ thống");
        }

        // Kiểm tra lecturerCode đã tồn tại chưa
        if (lecturerRepo.existsByLecturerCode(req.getLecturerCode())) {
            throw new ConflictException("Mã giảng viên đã tồn tại trong hệ thống");
        }

        // Lấy OrgUnit nếu có
        OrgUnit orgUnit = null;
        if (req.getOrgUnitId() != null) {
            orgUnit = orgUnitRepo.findById(req.getOrgUnitId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn vị tổ chức"));
        }

        Instant now = Instant.now();

        // Tạo User với tất cả thông tin giảng viên
        User lecturer = User.builder()
                .email(req.getEmail())
                .fullName(req.getFullName())
                .role(ROLE_LECTURER)
                .passwordHash(encoder.encode(req.getPassword()))
                .avatarUrl(req.getAvatarUrl())
                .lecturerCode(req.getLecturerCode())
                .primaryOrgUnit(orgUnit)
                .academicDegree(req.getAcademicDegree())
                .academicRank(req.getAcademicRank())
                .specialization(req.getSpecialization())
                .createdAt(now)
                .updatedAt(now)
                .build();

        lecturerRepo.save(lecturer);

        return lecturerMapper.toLecturerResponse(lecturer);
    }

    /**
     * Cập nhật thông tin giảng viên
     */
    @Transactional
    public LecturerResponse update(UUID id, UpdateLecturerRequest req) {
        User lecturer = lecturerRepo.findLecturerById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giảng viên"));

        // Cập nhật email nếu có thay đổi
        if (req.getEmail() != null && !req.getEmail().equals(lecturer.getEmail())) {
            if (lecturerRepo.findByEmail(req.getEmail()).isPresent()) {
                throw new ConflictException("Email đã tồn tại trong hệ thống");
            }
            lecturer.setEmail(req.getEmail());
        }

        // Cập nhật các field User
        if (req.getFullName() != null) {
            lecturer.setFullName(req.getFullName());
        }

        if (req.getPassword() != null) {
            lecturer.setPasswordHash(encoder.encode(req.getPassword()));
        }

        if (req.getAvatarUrl() != null) {
            lecturer.setAvatarUrl(req.getAvatarUrl());
        }

        // Kiểm tra lecturerCode mới có trùng không
        if (req.getLecturerCode() != null && !req.getLecturerCode().equals(lecturer.getLecturerCode())) {
            if (lecturerRepo.existsByLecturerCode(req.getLecturerCode())) {
                throw new ConflictException("Mã giảng viên đã tồn tại trong hệ thống");
            }
            lecturer.setLecturerCode(req.getLecturerCode());
        }

        // Cập nhật OrgUnit
        if (req.getOrgUnitId() != null) {
            OrgUnit orgUnit = orgUnitRepo.findById(req.getOrgUnitId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn vị tổ chức"));
            lecturer.setPrimaryOrgUnit(orgUnit);
        }

        if (req.getAcademicDegree() != null) {
            lecturer.setAcademicDegree(req.getAcademicDegree());
        }

        if (req.getAcademicRank() != null) {
            lecturer.setAcademicRank(req.getAcademicRank());
        }

        if (req.getSpecialization() != null) {
            lecturer.setSpecialization(req.getSpecialization());
        }

        lecturer.setUpdatedAt(Instant.now());
        lecturerRepo.save(lecturer);

        return lecturerMapper.toLecturerResponse(lecturer);
    }

    /**
     * Xóa giảng viên
     */
    @Transactional
    public void delete(UUID id) {
        User lecturer = lecturerRepo.findLecturerById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giảng viên"));

        lecturerRepo.delete(lecturer);
    }
}
