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
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.admin.LecturerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LecturerService {

    private static final String ROLE_LECTURER = "LECTURER";

    private final LecturerRepository lecturerRepo;
    private final BCryptPasswordEncoder encoder;

    /**
     * Map User entity sang LecturerResponse DTO
     */
    private LecturerResponse mapToResponse(User user) {
        LecturerResponse response = new LecturerResponse();
        response.setId(user.getId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }

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

        Page<User> result = lecturerRepo.findAllLecturers(req.getQ(), pageable);

        return new PagedResponse<>(
                result.map(this::mapToResponse).getContent(),
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
                .map(this::mapToResponse)
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

        Instant now = Instant.now();

        User lecturer = User.builder()
                .email(req.getEmail())
                .fullName(req.getFullName())
                .role(ROLE_LECTURER)
                .passwordHash(encoder.encode(req.getPassword()))
                .avatarUrl(req.getAvatarUrl())
                .createdAt(now)
                .updatedAt(now)
                .build();

        lecturerRepo.save(lecturer);
        return mapToResponse(lecturer);
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

        // Cập nhật các field khác nếu có
        if (req.getFullName() != null) {
            lecturer.setFullName(req.getFullName());
        }

        if (req.getPassword() != null) {
            lecturer.setPasswordHash(encoder.encode(req.getPassword()));
        }

        if (req.getAvatarUrl() != null) {
            lecturer.setAvatarUrl(req.getAvatarUrl());
        }

        lecturer.setUpdatedAt(Instant.now());

        lecturerRepo.save(lecturer);
        return mapToResponse(lecturer);
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
