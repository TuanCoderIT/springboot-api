package com.example.springboot_api.services.admin;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.springboot_api.common.exceptions.NotFoundException;
import com.example.springboot_api.dto.admin.notebook.ListCommunityRequest;
import com.example.springboot_api.dto.admin.notebook.NotebookCreateRequest;
import com.example.springboot_api.dto.admin.notebook.NotebookResponse;
import com.example.springboot_api.dto.shared.PagedResponse;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.admin.NotebookMemberRepository;
import com.example.springboot_api.repositories.admin.NotebookRepository;
import com.example.springboot_api.repositories.admin.UserRepository;
import com.example.springboot_api.services.shared.FileStorageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminCommunityService {

    private final NotebookRepository notebookRepository;
    private final NotebookMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Value("${file.base-url}")
    private String baseUrl;

    @Transactional
    public NotebookResponse createCommunity(NotebookCreateRequest req, MultipartFile thumbnail, UUID adminId) {
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
        return mapToResponse(saved);
    }

    @Transactional
    public NotebookResponse update(UUID id, NotebookCreateRequest req, MultipartFile thumbnail) {
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
        String visibility = req.getVisibility() != null && !req.getVisibility().isEmpty() ? req.getVisibility() : null;

        // Nếu sort theo memberCount, cần xử lý đặc biệt
        if ("memberCount".equals(sortBy)) {
            // Lấy tất cả communities (không phân trang) để sort theo memberCount
            Pageable allPageable = PageRequest.of(0, Integer.MAX_VALUE);
            Page<Notebook> allResult = notebookRepository.findCommunities(keyword, visibility, allPageable);

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
            Page<Notebook> result = notebookRepository.findCommunities(keyword, visibility, pageable);

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
        Long memberCount = memberRepository.countByNotebookIdAndStatus(nb.getId(), "approved");

        // Convert relative path to full URL if needed
        String thumbnailUrl = nb.getThumbnailUrl();
        if (thumbnailUrl != null) {
            // Normalize URL: convert /files/notebooks/ hoặc /files/ thành /uploads/
            if (thumbnailUrl.contains("/files/notebooks/")) {
                thumbnailUrl = thumbnailUrl.replace("/files/notebooks/", "/uploads/");
            } else if (thumbnailUrl.contains("/files/")) {
                thumbnailUrl = thumbnailUrl.replace("/files/", "/uploads/");
            }

            // Nếu đã là full URL (bắt đầu bằng http:// hoặc https://) thì giữ nguyên
            if (!thumbnailUrl.startsWith("http://") && !thumbnailUrl.startsWith("https://")) {
                // Nếu là relative path (bắt đầu bằng /) thì thêm baseUrl
                if (thumbnailUrl.startsWith("/")) {
                    thumbnailUrl = baseUrl + thumbnailUrl;
                }
            }
        }

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
}