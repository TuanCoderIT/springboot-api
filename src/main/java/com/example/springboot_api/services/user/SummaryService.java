package com.example.springboot_api.services.user;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot_api.common.exceptions.BadRequestException;
import com.example.springboot_api.common.exceptions.NotFoundException;
import com.example.springboot_api.dto.user.summary.SummaryResponse;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookAiSet;
import com.example.springboot_api.models.NotebookAiSetFile;
import com.example.springboot_api.models.NotebookAiSummary;
import com.example.springboot_api.models.NotebookFile;
import com.example.springboot_api.models.NotebookMember;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.admin.NotebookMemberRepository;
import com.example.springboot_api.repositories.admin.NotebookRepository;
import com.example.springboot_api.repositories.admin.UserRepository;
import com.example.springboot_api.repositories.shared.NotebookAiSetFileRepository;
import com.example.springboot_api.repositories.shared.NotebookAiSetRepository;
import com.example.springboot_api.repositories.shared.NotebookAiSummaryRepository;
import com.example.springboot_api.repositories.shared.NotebookFileRepository;
import com.example.springboot_api.services.shared.ai.generation.SummaryGenerationService;
import com.example.springboot_api.utils.UrlNormalizer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service xử lý các thao tác liên quan đến Summary (tóm tắt với TTS).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SummaryService {

    private final NotebookAiSummaryRepository summaryRepository;
    private final NotebookAiSetRepository aiSetRepository;
    private final NotebookAiSetFileRepository aiSetFileRepository;
    private final NotebookRepository notebookRepository;
    private final NotebookMemberRepository notebookMemberRepository;
    private final NotebookFileRepository notebookFileRepository;
    private final UserRepository userRepository;
    private final SummaryGenerationService summaryGenerationService;
    private final UrlNormalizer urlNormalizer;

    // ================================
    // GENERATE SUMMARY (ASYNC)
    // ================================

    public Map<String, Object> generateSummary(UUID notebookId, UUID userId, List<UUID> fileIds,
            String voiceId, String language, String additionalRequirements) {
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

            NotebookAiSet savedAiSet = createSummaryAiSet(notebook, user, selectedFiles, fileIds,
                    voiceId, language, additionalRequirements);

            result.put("aiSetId", savedAiSet.getId());
            result.put("status", "queued");
            result.put("message", "Summary đang được tạo ở nền. Sử dụng aiSetId để theo dõi tiến trình.");
            result.put("success", true);

            log.info("📤 [SUMMARY] Gọi async method - Thread: {}", Thread.currentThread().getName());

            summaryGenerationService.processSummaryGenerationAsync(
                    savedAiSet.getId(), notebookId, userId, fileIds, voiceId, language, additionalRequirements);

        } catch (Exception e) {
            result.put("error", "Lỗi khi khởi tạo summary: " + e.getMessage());
            log.error("❌ [SUMMARY] Error: {}", e.getMessage(), e);
        }

        return result;
    }

    @Transactional
    public NotebookAiSet createSummaryAiSet(Notebook notebook, User user, List<NotebookFile> selectedFiles,
            List<UUID> fileIds, String voiceId, String language, String additionalRequirements) {

        OffsetDateTime now = OffsetDateTime.now();
        Map<String, Object> inputConfig = new HashMap<>();
        inputConfig.put("fileIds", fileIds);
        if (voiceId != null && !voiceId.isBlank()) {
            inputConfig.put("voiceId", voiceId);
        }
        if (language != null && !language.isBlank()) {
            inputConfig.put("language", language);
        }
        if (additionalRequirements != null && !additionalRequirements.trim().isEmpty()) {
            inputConfig.put("additionalRequirements", additionalRequirements.trim());
        }

        NotebookAiSet aiSet = NotebookAiSet.builder()
                .notebook(notebook)
                .createdBy(user)
                .setType("summary")
                .status("queued")
                .title("Tóm tắt từ " + selectedFiles.size() + " tài liệu")
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
    // GET SUMMARY BY AI SET ID
    // ================================

    @Transactional(readOnly = true)
    public SummaryResponse getSummaryByAiSetId(UUID userId, UUID notebookId, UUID notebookAiSetId) {
        Notebook notebook = notebookRepository.findById(notebookId)
                .orElseThrow(() -> new NotFoundException("Notebook không tồn tại"));

        Optional<NotebookMember> memberOpt = notebookMemberRepository.findByNotebookIdAndUserId(notebookId, userId);
        boolean isCommunity = "community".equals(notebook.getType());
        boolean isMember = memberOpt.isPresent() && "approved".equals(memberOpt.get().getStatus());

        if (isCommunity) {
            if (!isMember) {
                throw new BadRequestException("Bạn chưa tham gia nhóm cộng đồng này hoặc yêu cầu chưa được duyệt");
            }
        } else {
            if (!isMember) {
                throw new BadRequestException("Bạn chưa tham gia nhóm này");
            }
        }

        NotebookAiSet aiSet = aiSetRepository.findById(notebookAiSetId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy AI Set với ID: " + notebookAiSetId));

        if (aiSet.getNotebook() == null || !aiSet.getNotebook().getId().equals(notebookId)) {
            throw new BadRequestException("AI Set không thuộc notebook này");
        }

        if (!"summary".equals(aiSet.getSetType())) {
            throw new BadRequestException("AI Set này không phải là summary");
        }

        NotebookAiSummary summary = summaryRepository.findById(notebookAiSetId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy summary với ID: " + notebookAiSetId));

        return toSummaryResponse(summary, aiSet);
    }

    private SummaryResponse toSummaryResponse(NotebookAiSummary summary, NotebookAiSet aiSet) {
        String audioUrl = summary.getAudioUrl();
        if (audioUrl != null && !audioUrl.isBlank()) {
            audioUrl = urlNormalizer.normalizeToFull(audioUrl);
        }

        return SummaryResponse.builder()
                .id(summary.getId())
                .title(aiSet.getTitle())
                .contentMd(summary.getContentMd())
                .scriptTts(summary.getScriptTts())
                .language(summary.getLanguage())
                .audioUrl(audioUrl)
                .audioFormat(summary.getAudioFormat())
                .audioDurationMs(summary.getAudioDurationMs())
                .ttsProvider(summary.getTtsProvider())
                .ttsModel(summary.getTtsModel())
                .voiceId(summary.getVoiceId())
                .voiceLabel(summary.getVoiceLabel())
                .voiceSpeed(summary.getVoiceSpeed())
                .voicePitch(summary.getVoicePitch())
                .createdAt(summary.getCreatedAt())
                .createdBy(summary.getCreateBy() != null ? summary.getCreateBy().getId() : null)
                .hasAudio(audioUrl != null && !audioUrl.isBlank())
                .build();
    }
}
