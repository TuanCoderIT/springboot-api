package com.example.springboot_api.services.admin;

import java.time.OffsetDateTime;
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
import com.example.springboot_api.dto.admin.orgunit.CreateOrgUnitRequest;
import com.example.springboot_api.dto.admin.orgunit.ListOrgUnitRequest;
import com.example.springboot_api.dto.admin.orgunit.OrgUnitResponse;
import com.example.springboot_api.dto.admin.orgunit.UpdateOrgUnitRequest;
import com.example.springboot_api.dto.shared.PagedResponse;
import com.example.springboot_api.mappers.OrgUnitMapper;
import com.example.springboot_api.models.OrgUnit;
import com.example.springboot_api.repositories.shared.OrgUnitRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrgUnitService {

    private final OrgUnitRepository orgUnitRepo;
    private final OrgUnitMapper orgUnitMapper;

    /**
     * Lấy danh sách đơn vị tổ chức với phân trang và filter.
     */
    @Transactional(readOnly = true)
    public PagedResponse<OrgUnitResponse> list(ListOrgUnitRequest req) {
        String sortBy = Optional.ofNullable(req.getSortBy()).orElse("createdAt");
        String sortDir = Optional.ofNullable(req.getSortDir()).orElse("desc");

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(req.getPage(), req.getSize(), sort);

        // Gọi query với filter
        Page<OrgUnit> result = orgUnitRepo.findAllWithFilters(
                req.getQ(),
                req.getType(),
                req.getIsActive(),
                pageable);

        return new PagedResponse<>(
                result.map(orgUnitMapper::toOrgUnitResponse).getContent(),
                new PagedResponse.Meta(
                        result.getNumber(),
                        result.getSize(),
                        result.getTotalElements(),
                        result.getTotalPages()));
    }

    /**
     * Lấy thông tin chi tiết một đơn vị tổ chức.
     */
    @Transactional(readOnly = true)
    public OrgUnitResponse getOne(UUID id) {
        return orgUnitRepo.findById(id)
                .map(orgUnitMapper::toOrgUnitResponse)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn vị tổ chức"));
    }

    /**
     * Tạo đơn vị tổ chức mới.
     */
    @Transactional
    public OrgUnitResponse create(CreateOrgUnitRequest req) {
        // Kiểm tra code đã tồn tại chưa
        if (orgUnitRepo.existsByCode(req.getCode())) {
            throw new ConflictException("Mã đơn vị đã tồn tại trong hệ thống");
        }

        // Lấy parent nếu có
        OrgUnit parent = null;
        if (req.getParentId() != null) {
            parent = orgUnitRepo.findById(req.getParentId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn vị cha"));
        }

        OffsetDateTime now = OffsetDateTime.now();

        OrgUnit orgUnit = OrgUnit.builder()
                .code(req.getCode())
                .name(req.getName())
                .type(req.getType())
                .parent(parent)
                .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        orgUnitRepo.save(orgUnit);

        return orgUnitMapper.toOrgUnitResponse(orgUnit);
    }

    /**
     * Cập nhật đơn vị tổ chức.
     */
    @Transactional
    public OrgUnitResponse update(UUID id, UpdateOrgUnitRequest req) {
        OrgUnit orgUnit = orgUnitRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn vị tổ chức"));

        // Cập nhật code nếu có thay đổi
        if (req.getCode() != null && !req.getCode().equals(orgUnit.getCode())) {
            if (orgUnitRepo.existsByCode(req.getCode())) {
                throw new ConflictException("Mã đơn vị đã tồn tại trong hệ thống");
            }
            orgUnit.setCode(req.getCode());
        }

        if (req.getName() != null) {
            orgUnit.setName(req.getName());
        }

        if (req.getType() != null) {
            orgUnit.setType(req.getType());
        }

        if (req.getParentId() != null) {
            // Không cho phép set parent là chính nó
            if (req.getParentId().equals(id)) {
                throw new ConflictException("Không thể đặt đơn vị cha là chính nó");
            }
            OrgUnit parent = orgUnitRepo.findById(req.getParentId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn vị cha"));
            orgUnit.setParent(parent);
        }

        if (req.getIsActive() != null) {
            orgUnit.setIsActive(req.getIsActive());
        }

        orgUnit.setUpdatedAt(OffsetDateTime.now());
        orgUnitRepo.save(orgUnit);

        return orgUnitMapper.toOrgUnitResponse(orgUnit);
    }

    /**
     * Xóa đơn vị tổ chức.
     */
    @Transactional
    public void delete(UUID id) {
        OrgUnit orgUnit = orgUnitRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn vị tổ chức"));

        orgUnitRepo.delete(orgUnit);
    }
}
