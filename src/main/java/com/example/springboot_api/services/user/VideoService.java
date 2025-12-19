package com.example.springboot_api.services.user;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot_api.common.exceptions.BadRequestException;
import com.example.springboot_api.common.exceptions.NotFoundException;
import com.example.springboot_api.dto.user.video.VideoResponse;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookAiSet;
import com.example.springboot_api.models.NotebookAiSetFile;
import com.example.springboot_api.models.NotebookFile;
import com.example.springboot_api.models.NotebookMember;
import com.example.springboot_api.models.User;
import com.example.springboot_api.models.VideoAsset;
import com.example.springboot_api.repositories.admin.NotebookMemberRepository;
import com.example.springboot_api.repositories.admin.NotebookRepository;
import com.example.springboot_api.repositories.admin.UserRepository;
import com.example.springboot_api.repositories.shared.NotebookAiSetFileRepository;
import com.example.springboot_api.repositories.shared.NotebookAiSetRepository;
import com.example.springboot_api.repositories.shared.NotebookFileRepository;
import com.example.springboot_api.repositories.shared.VideoAssetRepository;
import com.example.springboot_api.services.shared.ai.generation.VideoGenerationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service xử lý các tính năng liên quan đến Video.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoService {

    private final VideoAssetRepository videoAssetRepository;
    private final NotebookAiSetRepository aiSetRepository;
    private final NotebookAiSetFileRepository aiSetFileRepository;
    private final NotebookMemberRepository memberRepository;
    private final NotebookRepository notebookRepository;
    private final NotebookFileRepository notebookFileRepository;
    private final UserRepository userRepository;
    private final VideoGenerationService videoGenerationService;
    private final com.example.springboot_api.utils.UrlNormalizer urlNormalizer;

    // ================================
    // GENERATE VIDEO (ASYNC)
    // ================================

    public Map<String, Object> generateVideo(UUID notebookId, UUID userId, List<UUID> fileIds,
            int numberOfSlides, boolean generateImages, String additionalRequirements) {
        Map<String, Object> result = new HashMap<>();

        try {
            Notebook notebook = notebookRepository.findById(notebookId)
                    .orElseThrow(() -> new NotFoundException("Notebook không tồn tại: " + notebookId));

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User không tồn tại: " + userId));

            if (fileIds == null || fileIds.isEmpty()) {
                result.put("error", "Danh sách file IDs không được để trống");
                return result;
            }

            List<NotebookFile> selectedFiles = new ArrayList<>();
            for (UUID fileId : fileIds) {
                NotebookFile file = notebookFileRepository.findById(fileId).orElse(null);
                if (file != null && file.getNotebook() != null && file.getNotebook().getId().equals(notebookId)) {
                    selectedFiles.add(file);
                }
            }

            if (selectedFiles.isEmpty()) {
                result.put("error", "Không tìm thấy file hợp lệ nào");
                return result;
            }

            NotebookAiSet savedAiSet = createVideoAiSet(notebook, user, selectedFiles, fileIds,
                    numberOfSlides, generateImages, additionalRequirements);

            result.put("aiSetId", savedAiSet.getId());
            result.put("status", "queued");
            result.put("message", "Video đang được tạo ở nền. Sử dụng aiSetId để theo dõi tiến trình.");
            result.put("success", true);

            log.info("📤 [VIDEO] Gọi async method - Thread: {}", Thread.currentThread().getName());

            videoGenerationService.processVideoGenerationAsync(
                    savedAiSet.getId(), notebookId, userId, fileIds,
                    "CORPORATE", additionalRequirements, numberOfSlides, generateImages);

        } catch (Exception e) {
            result.put("error", "Lỗi khi khởi tạo video: " + e.getMessage());
            log.error("❌ [VIDEO] Error: {}", e.getMessage(), e);
        }

        return result;
    }

    @Transactional
    public NotebookAiSet createVideoAiSet(Notebook notebook, User user, List<NotebookFile> selectedFiles,
            List<UUID> fileIds, int numberOfSlides, boolean generateImages, String additionalRequirements) {

        OffsetDateTime now = OffsetDateTime.now();
        Map<String, Object> inputConfig = new HashMap<>();
        inputConfig.put("fileIds", fileIds);
        inputConfig.put("numberOfSlides", numberOfSlides > 0 ? numberOfSlides : 5);
        inputConfig.put("generateImages", generateImages);
        if (additionalRequirements != null && !additionalRequirements.trim().isEmpty()) {
            inputConfig.put("additionalRequirements", additionalRequirements.trim());
        }

        NotebookAiSet aiSet = NotebookAiSet.builder()
                .notebook(notebook)
                .createdBy(user)
                .setType("video")
                .status("queued")
                .title("Video từ " + selectedFiles.size() + " tài liệu")
                .inputConfig(inputConfig)
                .createdAt(now)
                .updatedAt(now)
                .build();
        NotebookAiSet savedAiSet = aiSetRepository.save(aiSet);

        for (NotebookFile file : selectedFiles) {
            NotebookAiSetFile aiSetFile = NotebookAiSetFile.builder()
                    .aiSet(savedAiSet)
                    .file(file)
                    .createdAt(now)
                    .build();
            aiSetFileRepository.save(aiSetFile);
        }

        return savedAiSet;
    }

    // ================================
    // GET VIDEO BY AI SET ID
    // ================================

    @Transactional(readOnly = true)
    public VideoResponse getVideoByAiSetId(UUID userId, UUID notebookId, UUID aiSetId) {
        if (!notebookRepository.existsById(notebookId)) {
            throw new NotFoundException("Notebook không tồn tại");
        }

        validateMembership(userId, notebookId);

        NotebookAiSet aiSet = aiSetRepository.findById(aiSetId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy AI Set: " + aiSetId));

        if (aiSet.getNotebook() == null || !aiSet.getNotebook().getId().equals(notebookId)) {
            throw new BadRequestException("AI Set không thuộc notebook này");
        }

        if (!"video".equals(aiSet.getSetType())) {
            throw new BadRequestException("AI Set này không phải loại video");
        }

        VideoAsset video = videoAssetRepository.findByAiSetId(aiSetId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy video cho AI Set: " + aiSetId));

        return VideoResponse.builder()
                .id(video.getId())
                .aiSetId(aiSetId)
                .videoUrl(urlNormalizer.normalizeToFull(video.getVideoUrl()))
                .title(video.getTextSource())
                .style(video.getStyle())
                .durationSeconds(video.getDurationSeconds())
                .createdAt(video.getCreatedAt())
                .build();
    }

    private void validateMembership(UUID userId, UUID notebookId) {
        NotebookMember member = memberRepository.findByNotebookIdAndUserId(notebookId, userId)
                .orElseThrow(() -> new BadRequestException("Bạn không phải thành viên của notebook này."));

        if (!"approved".equals(member.getStatus())) {
            throw new BadRequestException("Bạn chưa được duyệt tham gia notebook này.");
        }
    }
}
