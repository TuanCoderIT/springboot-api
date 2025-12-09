package com.example.springboot_api.services.admin;

import java.io.IOException;
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
import org.springframework.web.multipart.MultipartFile;

import com.example.springboot_api.common.exceptions.BadRequestException;
import com.example.springboot_api.common.exceptions.NotFoundException;
import com.example.springboot_api.dto.admin.notebook.ApproveRejectBlockRequest;
import com.example.springboot_api.dto.admin.notebook.ListCommunityRequest;
import com.example.springboot_api.dto.admin.notebook.MemberResponse;
import com.example.springboot_api.dto.admin.notebook.NotebookCreateRequest;
import com.example.springboot_api.dto.admin.notebook.NotebookResponse;
import com.example.springboot_api.dto.admin.notebook.PendingRequestResponse;
import com.example.springboot_api.dto.shared.PagedResponse;
import com.example.springboot_api.dto.user.community.NotebookDetailResponse;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookMember;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.admin.NotebookMemberRepository;
import com.example.springboot_api.repositories.admin.NotebookRepository;
import com.example.springboot_api.repositories.admin.UserRepository;
import com.example.springboot_api.repositories.shared.FlashcardRepository;
import com.example.springboot_api.repositories.shared.NotebookBotConversationRepository;
import com.example.springboot_api.repositories.shared.NotebookFileRepository;
import com.example.springboot_api.repositories.shared.NotebookMessageRepository;
import com.example.springboot_api.repositories.shared.QuizRepository;
import com.example.springboot_api.repositories.shared.TtsAssetRepository;
import com.example.springboot_api.repositories.shared.VideoAssetRepository;
import com.example.springboot_api.services.shared.FileStorageService;
import com.example.springboot_api.utils.UrlNormalizer;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminCommunityService {

    private final NotebookRepository notebookRepository;
    private final NotebookMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final NotebookFileRepository fileRepository;
    private final VideoAssetRepository videoAssetRepository;
    private final FlashcardRepository flashcardRepository;
    private final TtsAssetRepository ttsAssetRepository;
    private final QuizRepository quizRepository;
    private final NotebookMessageRepository messageRepository;
    private final NotebookBotConversationRepository notebookBotConversationRepository;
    private final FileStorageService fileStorageService;
    private final UrlNormalizer urlNormalizer;

    @Transactional
    public NotebookResponse createCommunity(NotebookCreateRequest req,
            MultipartFile thumbnail, UUID adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new NotFoundException("Admin not found"));

        Notebook nb = new Notebook();
        nb.setTitle(req.title());
        nb.setDescription(req.description());
        nb.setType("community");
        nb.setVisibility(req.visibility());
        nb.setCreatedBy(admin);

        // Xử lý upload thumbnail
        if (thumbnail != null && !thumbnail.isEmpty()) {
            try {
                String thumbnailUrl = fileStorageService.storeFile(thumbnail);
                nb.setThumbnailUrl(thumbnailUrl);
            } catch (IOException e) {
                throw new RuntimeException("Failed to upload thumbnail", e);
            }
        }

        Notebook saved = notebookRepository.save(nb);

        NotebookMember ownerMember = NotebookMember.builder()
                .notebook(saved)
                .user(admin)
                .role("owner")
                .status("approved")
                .joinedAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        memberRepository.save(ownerMember);

        return mapToResponse(saved);
    }

    @Transactional
    public NotebookResponse update(UUID id, NotebookCreateRequest req,
            MultipartFile thumbnail) {
        Notebook nb = notebookRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Community not found"));

        nb.setTitle(req.title());
        nb.setDescription(req.description());
        nb.setVisibility(req.visibility());

        // Xử lý upload thumbnail mới
        if (thumbnail != null && !thumbnail.isEmpty()) {
            // Xóa thumbnail cũ nếu có
            if (nb.getThumbnailUrl() != null) {
                fileStorageService.deleteFile(nb.getThumbnailUrl());
            }

            try {
                String thumbnailUrl = fileStorageService.storeFile(thumbnail);
                nb.setThumbnailUrl(thumbnailUrl);
            } catch (IOException e) {
                throw new RuntimeException("Failed to upload thumbnail", e);
            }
        }

        Notebook saved = notebookRepository.save(nb);
        return mapToResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        Notebook nb = notebookRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Community not found"));

        // Xóa thumbnail nếu có
        if (nb.getThumbnailUrl() != null) {
            fileStorageService.deleteFile(nb.getThumbnailUrl());
        }

        notebookRepository.deleteById(id);
    }

    public NotebookResponse getOne(UUID id) {
        Notebook nb = notebookRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Community not found"));
        return mapToResponse(nb);
    }

    public PagedResponse<NotebookResponse> list(ListCommunityRequest req) {
        String sortBy = Optional.ofNullable(req.getSortBy()).orElse("createdAt");
        String sortDir = Optional.ofNullable(req.getSortDir()).orElse("desc");

        String keyword = req.getQ() != null && !req.getQ().isEmpty() ? req.getQ() : null;
        String visibility = req.getVisibility() != null &&
                !req.getVisibility().isEmpty() ? req.getVisibility() : null;

        // Nếu sort theo memberCount, cần xử lý đặc biệt
        if ("memberCount".equals(sortBy)) {
            // Lấy tất cả communities (không phân trang) để sort theo memberCount
            Pageable allPageable = PageRequest.of(0, Integer.MAX_VALUE);
            Page<Notebook> allResult = notebookRepository.findCommunities(keyword,
                    visibility, allPageable);

            // Map và sort theo memberCount
            var sortedList = allResult.getContent().stream()
                    .map(this::mapToResponse)
                    .sorted((a, b) -> {
                        int compare = Long.compare(
                                a.memberCount() != null ? a.memberCount() : 0L,
                                b.memberCount() != null ? b.memberCount() : 0L);
                        return sortDir.equalsIgnoreCase("asc") ? compare : -compare;
                    })
                    .toList();

            // Phân trang thủ công
            int start = req.getPage() * req.getSize();
            int end = Math.min(start + req.getSize(), sortedList.size());
            java.util.List<NotebookResponse> pagedList = start < sortedList.size()
                    ? sortedList.subList(start, end)
                    : new java.util.ArrayList<>();

            int totalPages = (int) Math.ceil((double) sortedList.size() / req.getSize());

            return new PagedResponse<>(
                    pagedList,
                    new PagedResponse.Meta(
                            req.getPage(),
                            req.getSize(),
                            sortedList.size(),
                            totalPages));
        } else {
            // Sort theo các field của Notebook entity
            Sort sort = sortDir.equalsIgnoreCase("asc")
                    ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();

            Pageable pageable = PageRequest.of(req.getPage(), req.getSize(), sort);
            Page<Notebook> result = notebookRepository.findCommunities(keyword,
                    visibility, pageable);

            return new PagedResponse<>(
                    result.map(this::mapToResponse).getContent(),
                    new PagedResponse.Meta(
                            result.getNumber(),
                            result.getSize(),
                            result.getTotalElements(),
                            result.getTotalPages()));
        }
    }

    private NotebookResponse mapToResponse(Notebook nb) {
        Long memberCount = memberRepository.countByNotebookIdAndStatus(nb.getId(),
                "approved");

        // Normalize thumbnailUrl
        String thumbnailUrl = urlNormalizer.normalizeToFull(nb.getThumbnailUrl());

        return new NotebookResponse(
                nb.getId(),
                nb.getTitle(),
                nb.getDescription(),
                nb.getType(),
                nb.getVisibility(),
                thumbnailUrl,
                memberCount,
                nb.getCreatedAt(),
                nb.getUpdatedAt());
    }

    public PagedResponse<PendingRequestResponse> getPendingRequests(
            UUID notebookId, String status, String keyword, String sortBy, String sortDir, int page, int size) {

        String q = (keyword != null && !keyword.isEmpty()) ? keyword : null;
        String statusFilter = (status != null && !status.isEmpty()) ? status : "pending";

        String sortByField = Optional.ofNullable(sortBy).orElse("createdAt");
        String sortDirection = Optional.ofNullable(sortDir).orElse("desc");

        Sort sort = Sort.by(sortDirection.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC,
                sortByField);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<NotebookMember> result = memberRepository.findMembersWithFilters(notebookId, statusFilter, q,
                pageable);

        return new PagedResponse<>(
                result.map(this::mapToPendingRequest).getContent(),
                new PagedResponse.Meta(
                        result.getNumber(),
                        result.getSize(),
                        result.getTotalElements(),
                        result.getTotalPages()));
    }

    @Transactional
    public void approveRejectBlockMember(ApproveRejectBlockRequest req) {
        NotebookMember member = memberRepository
                .findByNotebookIdAndUserId(req.notebookId(), req.userId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thành viên"));

        if ("owner".equals(member.getRole()) &&
                "block".equals(req.action().toLowerCase())) {
            throw new BadRequestException("Không thể chặn chủ sở hữu");
        }

        String action = req.action().toLowerCase();
        String newStatus;

        switch (action) {
            case "approve":
                newStatus = "approved";
                if (member.getJoinedAt() == null) {
                    member.setJoinedAt(java.time.OffsetDateTime.now());
                }
                break;
            case "reject":
                newStatus = "rejected";
                break;
            case "block":
                newStatus = "blocked";
                break;
            default:
                throw new BadRequestException("Action không hợp lệ. Chỉ chấp nhận: approve, reject, block");
        }

        member.setStatus(newStatus);
        member.setUpdatedAt(java.time.OffsetDateTime.now());
        memberRepository.save(member);
    }

    public NotebookDetailResponse getNotebookDetail(UUID notebookId) {
        Notebook notebook = notebookRepository.findById(notebookId)
                .orElseThrow(() -> new NotFoundException("Nhóm không tồn tại"));

        // Lấy danh sách thành viên đã approved
        var approvedMembers = memberRepository.findApprovedMembers(notebookId);
        var memberItems = approvedMembers.stream()
                .map(m -> new NotebookDetailResponse.MemberItem(
                        m.getUser().getId(),
                        m.getUser().getFullName(),
                        m.getUser().getEmail(),
                        m.getRole(),
                        m.getStatus(),
                        m.getJoinedAt()))
                .toList();

        // Lấy danh sách files
        var files = fileRepository.findByNotebookId(notebookId);
        var fileItems = files.stream()
                .map(f -> new NotebookDetailResponse.FileItem(
                        f.getId(),
                        f.getOriginalFilename(),
                        f.getMimeType(),
                        f.getFileSize(),
                        urlNormalizer.normalizeToFull(f.getStorageUrl()),
                        f.getStatus(),
                        f.getCreatedAt()))
                .toList();

        String thumbnailUrl = urlNormalizer.normalizeToFull(notebook.getThumbnailUrl());

        return new NotebookDetailResponse(
                notebook.getId(),
                notebook.getTitle(),
                notebook.getDescription(),
                notebook.getType(),
                notebook.getVisibility(),
                thumbnailUrl,
                notebook.getCreatedBy().getId(),
                notebook.getCreatedBy().getFullName(),
                notebook.getCreatedAt(),
                notebook.getUpdatedAt(),
                new NotebookDetailResponse.MemberInfo(
                        (long) memberItems.size(),
                        memberItems),
                new NotebookDetailResponse.FileInfo(
                        (long) fileItems.size(),
                        fileItems));
    }

    public PagedResponse<MemberResponse> getNotebookMembers(
            UUID notebookId, String status, String keyword, String sortBy, String sortDir, int page, int size) {

        notebookRepository.findById(notebookId)
                .orElseThrow(() -> new NotFoundException("Nhóm không tồn tại"));

        String q = (keyword != null && !keyword.isEmpty()) ? keyword : null;
        String statusFilter = (status != null && !status.isEmpty()) ? status : null;

        String sortByField = Optional.ofNullable(sortBy).orElse("joinedAt");
        String sortDirection = Optional.ofNullable(sortDir).orElse("desc");

        Sort sort = Sort.by(sortDirection.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC,
                sortByField);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<NotebookMember> result = memberRepository.findByNotebookIdWithFilters(notebookId, statusFilter, q,
                pageable);

        return new PagedResponse<>(
                result.map(this::mapToMemberResponse).getContent(),
                new PagedResponse.Meta(
                        result.getNumber(),
                        result.getSize(),
                        result.getTotalElements(),
                        result.getTotalPages()));
    }

    @Transactional
    public void updateMemberRole(UUID memberId, String role) {
        NotebookMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thành viên"));

        if ("owner".equals(member.getRole())) {
            throw new BadRequestException("Không thể thay đổi role của chủ sở hữu");
        }

        if (!"admin".equals(role) && !"member".equals(role) && !"owner".equals(role)) {
            throw new BadRequestException("Role không hợp lệ. Chỉ chấp nhận: admin, member, owner");
        }

        if ("owner".equals(role)) {
            throw new BadRequestException("Không thể cấp role owner thông qua API này");
        }

        member.setRole(role);
        member.setUpdatedAt(OffsetDateTime.now());
        memberRepository.save(member);
    }

    @Transactional
    public void deleteMember(UUID memberId) {
        NotebookMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thành viên"));

        if ("owner".equals(member.getRole())) {
            throw new BadRequestException("Không thể xóa chủ sở hữu. Chủ sở hữu không thể bị xóa khỏi notebook.");
        }

        UUID notebookId = member.getNotebook().getId();
        UUID userId = member.getUser().getId();

        Long fileCount = fileRepository.countByNotebookIdAndUserId(notebookId,
                userId);
        Long videoCount = videoAssetRepository.countByNotebookIdAndUserId(notebookId,
                userId);
        Long flashcardCount = flashcardRepository.countByNotebookIdAndUserId(notebookId, userId);
        Long ttsCount = ttsAssetRepository.countByNotebookIdAndUserId(notebookId,
                userId);
        Long quizCount = quizRepository.countByNotebookIdAndUserId(notebookId,
                userId);
        Long messageCount = messageRepository.countByNotebookIdAndUserId(notebookId,
                userId);
        Long ragQueryCount = notebookBotConversationRepository.countByNotebookIdAndUserId(notebookId, userId);

        long totalContributions = fileCount + videoCount + flashcardCount + ttsCount
                + quizCount + messageCount
                + ragQueryCount;

        if (totalContributions > 0) {
            throw new BadRequestException(
                    "Không thể xóa thành viên đã có đóng góp. Vui lòng sử dụng chức năng chặn (block) để ẩn thành viên này.");
        }

        memberRepository.delete(member);
    }

    @Transactional
    public void blockMember(UUID memberId) {
        NotebookMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thành viên"));

        if ("owner".equals(member.getRole())) {
            throw new BadRequestException("Không thể chặn chủ sở hữu. Chủ sở hữu không thể bị chặn.");
        }

        member.setStatus("blocked");
        member.setUpdatedAt(OffsetDateTime.now());
        memberRepository.save(member);
    }

    @Transactional
    public void unblockMember(UUID memberId) {
        NotebookMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thành viên"));

        if (!"blocked".equals(member.getStatus())) {
            throw new BadRequestException(
                    "Thành viên này không bị chặn. Chỉ có thể mở chặn cho thành viên đang bị chặn.");
        }

        member.setStatus("approved");
        if (member.getJoinedAt() == null) {
            member.setJoinedAt(OffsetDateTime.now());
        }
        member.setUpdatedAt(OffsetDateTime.now());
        memberRepository.save(member);
    }

    @Transactional
    public void approveMember(UUID memberId) {
        NotebookMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thành viên"));

        if (!"pending".equals(member.getStatus())) {
            throw new BadRequestException(
                    "Chỉ có thể phê duyệt yêu cầu đang ở trạng thái pending. Trạng thái hiện tại: "
                            + member.getStatus());
        }

        member.setStatus("approved");
        if (member.getJoinedAt() == null) {
            member.setJoinedAt(OffsetDateTime.now());
        }
        member.setUpdatedAt(OffsetDateTime.now());
        memberRepository.save(member);
    }

    @Transactional
    public int approveAllPendingRequests(UUID notebookId) {
        List<NotebookMember> pendingMembers = memberRepository.findAllPendingRequests(notebookId);

        if (pendingMembers.isEmpty()) {
            return 0;
        }

        OffsetDateTime now = OffsetDateTime.now();
        int approvedCount = 0;

        for (NotebookMember member : pendingMembers) {
            member.setStatus("approved");
            if (member.getJoinedAt() == null) {
                member.setJoinedAt(now);
            }
            member.setUpdatedAt(now);
            memberRepository.save(member);
            approvedCount++;
        }

        return approvedCount;
    }

    private PendingRequestResponse mapToPendingRequest(NotebookMember nm) {
        return new PendingRequestResponse(
                nm.getId(),
                nm.getNotebook().getId(),
                nm.getNotebook().getTitle(),
                nm.getUser().getId(),
                nm.getUser().getFullName(),
                nm.getUser().getEmail(),
                nm.getRole(),
                nm.getStatus(),
                nm.getJoinedAt(),
                nm.getCreatedAt(),
                nm.getUpdatedAt());
    }

    private MemberResponse mapToMemberResponse(NotebookMember nm) {
        UUID notebookId = nm.getNotebook().getId();
        UUID userId = nm.getUser().getId();

        Long fileCount = fileRepository.countByNotebookIdAndUserId(notebookId,
                userId);
        Long videoCount = videoAssetRepository.countByNotebookIdAndUserId(notebookId,
                userId);
        Long flashcardCount = flashcardRepository.countByNotebookIdAndUserId(notebookId, userId);
        Long ttsCount = ttsAssetRepository.countByNotebookIdAndUserId(notebookId,
                userId);
        Long quizCount = quizRepository.countByNotebookIdAndUserId(notebookId,
                userId);
        Long messageCount = messageRepository.countByNotebookIdAndUserId(notebookId,
                userId);
        Long ragQueryCount = notebookBotConversationRepository.countByNotebookIdAndUserId(notebookId, userId);

        var statistics = new MemberResponse.UserStatistics(
                fileCount,
                videoCount,
                flashcardCount,
                ttsCount,
                quizCount,
                messageCount,
                ragQueryCount);

        String avatarUrl = urlNormalizer.normalizeToFull(nm.getUser().getAvatarUrl());

        return new MemberResponse(
                nm.getId(),
                nm.getNotebook().getId(),
                nm.getNotebook().getTitle(),
                nm.getUser().getId(),
                nm.getUser().getFullName(),
                nm.getUser().getEmail(),
                avatarUrl,
                nm.getRole(),
                nm.getStatus(),
                nm.getJoinedAt(),
                nm.getCreatedAt(),
                nm.getUpdatedAt(),
                statistics);
    }

}