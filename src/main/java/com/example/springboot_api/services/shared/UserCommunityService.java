package com.example.springboot_api.services.shared;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot_api.common.exceptions.BadRequestException;
import com.example.springboot_api.common.exceptions.ConflictException;
import com.example.springboot_api.common.exceptions.NotFoundException;
import com.example.springboot_api.dto.shared.PagedResponse;
import com.example.springboot_api.dto.shared.community.AvailableGroupResponse;
import com.example.springboot_api.dto.shared.community.JoinGroupRequest;
import com.example.springboot_api.dto.shared.community.JoinGroupResponse;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookMember;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.admin.NotebookMemberRepository;
import com.example.springboot_api.repositories.admin.NotebookRepository;
import com.example.springboot_api.repositories.admin.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserCommunityService {

    private final NotebookRepository notebookRepository;
    private final NotebookMemberRepository memberRepository;
    private final UserRepository userRepository;

    @Value("${file.base-url}")
    private String baseUrl;

    @Transactional
    public JoinGroupResponse joinGroup(JoinGroupRequest req, UUID userId) {
        Notebook notebook = notebookRepository.findById(req.notebookId())
                .orElseThrow(() -> new NotFoundException("Nhóm không tồn tại"));

        if (!"community".equals(notebook.getType())) {
            throw new BadRequestException("Đây không phải là nhóm cộng đồng");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));

        // Kiểm tra đã tham gia chưa
        Optional<NotebookMember> existingMember = memberRepository.findByNotebookIdAndUserId(
                req.notebookId(), userId);

        if (existingMember.isPresent()) {
            NotebookMember member = existingMember.get();
            if ("approved".equals(member.getStatus())) {
                throw new ConflictException("Bạn đã tham gia nhóm này");
            }
            if ("pending".equals(member.getStatus())) {
                throw new ConflictException("Yêu cầu tham gia đang chờ duyệt");
            }
            if ("blocked".equals(member.getStatus())) {
                throw new BadRequestException("Bạn đã bị chặn khỏi nhóm này");
            }
        }

        // Xử lý theo visibility
        String status;
        String message;
        OffsetDateTime joinedAt = null;

        if ("blocked".equals(notebook.getVisibility())) {
            throw new BadRequestException("Nhóm này đã bị khóa");
        } else if ("public".equals(notebook.getVisibility())) {
            // Auto approved
            status = "approved";
            message = "Tham gia nhóm thành công";
            joinedAt = OffsetDateTime.now();
        } else {
            // private - tạo request pending
            status = "pending";
            message = "Yêu cầu tham gia đã được gửi, đang chờ duyệt";
        }

        // Tạo hoặc cập nhật member
        NotebookMember member;
        if (existingMember.isPresent()) {
            member = existingMember.get();
            member.setStatus(status);
            if (joinedAt != null) {
                member.setJoinedAt(joinedAt);
            }
        } else {
            member = NotebookMember.builder()
                    .notebook(notebook)
                    .user(user)
                    .role("member")
                    .status(status)
                    .joinedAt(joinedAt)
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();
        }

        memberRepository.save(member);

        return new JoinGroupResponse(req.notebookId(), status, message);
    }

    public PagedResponse<AvailableGroupResponse> getAvailableGroups(
            UUID userId, String keyword, String visibility, int page, int size) {

        String q = (keyword != null && !keyword.isEmpty()) ? keyword : null;
        String vis = (visibility != null && !visibility.isEmpty()) ? visibility : null;

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Notebook> result = notebookRepository.findAvailableCommunities(userId, q, vis, pageable);

        return new PagedResponse<>(
                result.map(this::mapToAvailableGroup).getContent(),
                new PagedResponse.Meta(
                        result.getNumber(),
                        result.getSize(),
                        result.getTotalElements(),
                        result.getTotalPages()));
    }

    private AvailableGroupResponse mapToAvailableGroup(Notebook nb) {
        Long memberCount = memberRepository.countByNotebookIdAndStatus(nb.getId(), "approved");

        String thumbnailUrl = normalizeThumbnailUrl(nb.getThumbnailUrl());

        return new AvailableGroupResponse(
                nb.getId(),
                nb.getTitle(),
                nb.getDescription(),
                nb.getVisibility(),
                thumbnailUrl,
                memberCount,
                nb.getCreatedAt());
    }

    private String normalizeThumbnailUrl(String thumbnailUrl) {
        if (thumbnailUrl == null) {
            return null;
        }

        if (thumbnailUrl.contains("/files/notebooks/")) {
            thumbnailUrl = thumbnailUrl.replace("/files/notebooks/", "/uploads/");
        } else if (thumbnailUrl.contains("/files/")) {
            thumbnailUrl = thumbnailUrl.replace("/files/", "/uploads/");
        }

        if (!thumbnailUrl.startsWith("http://") && !thumbnailUrl.startsWith("https://")) {
            if (thumbnailUrl.startsWith("/")) {
                thumbnailUrl = baseUrl + thumbnailUrl;
            }
        }

        return thumbnailUrl;
    }
}

