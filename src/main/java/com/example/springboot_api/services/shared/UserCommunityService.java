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
import com.example.springboot_api.dto.shared.community.CommunityPreviewResponse;
import com.example.springboot_api.dto.shared.community.JoinGroupRequest;
import com.example.springboot_api.dto.shared.community.JoinGroupResponse;
import com.example.springboot_api.dto.shared.community.MembershipStatusResponse;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookMember;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.admin.NotebookMemberRepository;
import com.example.springboot_api.repositories.admin.NotebookRepository;
import com.example.springboot_api.repositories.admin.UserRepository;
import com.example.springboot_api.repositories.shared.FlashcardRepository;
import com.example.springboot_api.repositories.shared.NotebookFileRepository;
import com.example.springboot_api.repositories.shared.NotebookMessageRepository;
import com.example.springboot_api.repositories.shared.QuizRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserCommunityService {

    private final NotebookRepository notebookRepository;
    private final NotebookMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final NotebookFileRepository fileRepository;
    private final NotebookMessageRepository messageRepository;
    private final QuizRepository quizRepository;
    private final FlashcardRepository flashcardRepository;

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
            UUID userId, String keyword, String sortBy, String sortDir, int page, int size) {

        String q = (keyword != null && !keyword.isEmpty()) ? keyword : null;
        String visibility = "public";

        String sortByField = Optional.ofNullable(sortBy).orElse("createdAt");
        String sortDirection = Optional.ofNullable(sortDir).orElse("desc");

        if ("memberCount".equals(sortByField)) {
            Pageable allPageable = PageRequest.of(0, Integer.MAX_VALUE);
            Page<Notebook> allResult = notebookRepository.findAvailableCommunities(userId, q, visibility, allPageable);

            var sortedList = allResult.getContent().stream()
                    .map(this::mapToAvailableGroup)
                    .sorted((a, b) -> {
                        int compare = Long.compare(
                                a.memberCount() != null ? a.memberCount() : 0L,
                                b.memberCount() != null ? b.memberCount() : 0L);
                        return sortDirection.equalsIgnoreCase("asc") ? compare : -compare;
                    })
                    .toList();

            int start = page * size;
            int end = Math.min(start + size, sortedList.size());
            java.util.List<AvailableGroupResponse> pagedList = start < sortedList.size()
                    ? sortedList.subList(start, end)
                    : new java.util.ArrayList<>();

            int totalPages = (int) Math.ceil((double) sortedList.size() / size);

            return new PagedResponse<>(
                    pagedList,
                    new PagedResponse.Meta(
                            page,
                            size,
                            sortedList.size(),
                            totalPages));
        } else {
            Sort sort = sortDirection.equalsIgnoreCase("asc")
                    ? Sort.by(sortByField).ascending()
                    : Sort.by(sortByField).descending();

            Pageable pageable = PageRequest.of(page, size, sort);
            Page<Notebook> result = notebookRepository.findAvailableCommunities(userId, q, visibility, pageable);

            return new PagedResponse<>(
                    result.map(this::mapToAvailableGroup).getContent(),
                    new PagedResponse.Meta(
                            result.getNumber(),
                            result.getSize(),
                            result.getTotalElements(),
                            result.getTotalPages()));
        }
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

    public CommunityPreviewResponse getCommunityPreview(UUID notebookId) {
        Notebook notebook = notebookRepository.findById(notebookId)
                .orElseThrow(() -> new NotFoundException("Nhóm không tồn tại"));

        if (!"community".equals(notebook.getType())) {
            throw new BadRequestException("Đây không phải là nhóm cộng đồng");
        }

        if (!"public".equals(notebook.getVisibility())) {
            throw new BadRequestException("Chỉ có thể xem preview nhóm công khai");
        }

        Long memberCount = memberRepository.countByNotebookIdAndStatus(notebookId, "approved");
        Long fileCount = fileRepository.countByNotebookIdAndStatus(notebookId, "approved");
        Long messageCount = messageRepository.countByNotebookId(notebookId);
        Long flashcardCount = flashcardRepository.countByNotebookId(notebookId);
        Long quizCount = quizRepository.countByNotebookId(notebookId);

        var statistics = new CommunityPreviewResponse.Statistics(
                memberCount,
                fileCount,
                messageCount,
                flashcardCount,
                quizCount);

        var recentMessages = messageRepository.findRecentByNotebookId(
                notebookId,
                PageRequest.of(0, 5)).stream()
                .map(m -> new CommunityPreviewResponse.RecentMessagePreview(
                        m.getId(),
                        m.getType(),
                        truncateContent(m.getContent(), 100),
                        m.getUser() != null ? m.getUser().getFullName() : "Anonymous",
                        m.getCreatedAt()))
                .toList();

        var recentFiles = fileRepository.findApprovedByNotebookId(notebookId).stream()
                .limit(5)
                .map(f -> new CommunityPreviewResponse.FilePreview(
                        f.getId(),
                        f.getOriginalFilename(),
                        f.getMimeType(),
                        f.getFileSize(),
                        f.getCreatedAt()))
                .toList();

        String thumbnailUrl = normalizeThumbnailUrl(notebook.getThumbnailUrl());

        return new CommunityPreviewResponse(
                notebook.getId(),
                notebook.getTitle(),
                notebook.getDescription(),
                notebook.getVisibility(),
                thumbnailUrl,
                notebook.getCreatedBy() != null ? notebook.getCreatedBy().getId() : null,
                notebook.getCreatedBy() != null ? notebook.getCreatedBy().getFullName() : null,
                notebook.getCreatedAt(),
                notebook.getUpdatedAt(),
                statistics,
                recentMessages,
                recentFiles);
    }

    public MembershipStatusResponse checkMembershipStatus(UUID notebookId, UUID userId) {
        Notebook notebook = notebookRepository.findById(notebookId)
                .orElseThrow(() -> new NotFoundException("Nhóm không tồn tại"));

        if (!"community".equals(notebook.getType())) {
            throw new BadRequestException("Đây không phải là nhóm cộng đồng");
        }

        Optional<NotebookMember> memberOpt = memberRepository.findByNotebookIdAndUserId(notebookId, userId);

        if (memberOpt.isEmpty()) {
            boolean canJoin = !"blocked".equals(notebook.getVisibility());
            return new MembershipStatusResponse(
                    notebookId,
                    false,
                    canJoin,
                    null,
                    null,
                    null,
                    null);
        }

        NotebookMember member = memberOpt.get();
        String status = member.getStatus();
        boolean isMember = "approved".equals(status);

        boolean canJoin = false;
        if (!isMember) {
            if ("pending".equals(status)) {
                canJoin = false;
            } else if ("rejected".equals(status)) {
                canJoin = true;
            } else if ("blocked".equals(status)) {
                canJoin = false;
            }
        }

        return new MembershipStatusResponse(
                notebookId,
                isMember,
                canJoin,
                status,
                member.getRole(),
                member.getJoinedAt(),
                member.getCreatedAt());
    }

    private String truncateContent(String content, int maxLength) {
        if (content == null) {
            return null;
        }
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }
}
