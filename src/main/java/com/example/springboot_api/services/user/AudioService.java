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
import com.example.springboot_api.dto.user.audio.AudioListResponse;
import com.example.springboot_api.dto.user.audio.AudioResponse;
import com.example.springboot_api.mappers.AudioMapper;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookAiSet;
import com.example.springboot_api.models.NotebookAiSetFile;
import com.example.springboot_api.models.NotebookFile;
import com.example.springboot_api.models.NotebookMember;
import com.example.springboot_api.models.TtsAsset;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.admin.NotebookMemberRepository;
import com.example.springboot_api.repositories.admin.NotebookRepository;
import com.example.springboot_api.repositories.admin.UserRepository;
import com.example.springboot_api.repositories.shared.NotebookAiSetFileRepository;
import com.example.springboot_api.repositories.shared.NotebookAiSetRepository;
import com.example.springboot_api.repositories.shared.NotebookFileRepository;
import com.example.springboot_api.repositories.shared.TtsAssetRepository;
import com.example.springboot_api.services.shared.ai.generation.AudioOverviewService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service xử lý các thao tác liên quan đến Audio (TTS).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AudioService {

    private final TtsAssetRepository ttsAssetRepository;
    private final NotebookAiSetRepository aiSetRepository;
    private final NotebookAiSetFileRepository aiSetFileRepository;
    private final NotebookRepository notebookRepository;
    private final NotebookMemberRepository notebookMemberRepository;
    private final NotebookFileRepository notebookFileRepository;
    private final UserRepository userRepository;
    private final AudioMapper audioMapper;
    private final AudioOverviewService audioOverviewService;

    // ================================
    // GENERATE AUDIO OVERVIEW (ASYNC)
    // ================================

    public Map<String, Object> generateAudioOverview(UUID notebookId, UUID userId, List<UUID> fileIds,
            String voiceId, String outputFormat, String notes) {
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

            OffsetDateTime now = OffsetDateTime.now();
            Map<String, Object> inputConfig = new HashMap<>();
            inputConfig.put("fileIds", fileIds);
            if (voiceId != null && !voiceId.isBlank()) {
                inputConfig.put("voiceId", voiceId);
            }
            if (outputFormat != null && !outputFormat.isBlank()) {
                inputConfig.put("outputFormat", outputFormat);
            }
            if (notes != null && !notes.isBlank()) {
                inputConfig.put("notes", notes);
            }

            NotebookAiSet aiSet = NotebookAiSet.builder()
                    .notebook(notebook)
                    .createdBy(user)
                    .setType("tts")
                    .status("queued")
                    .title("Audio Overview từ " + selectedFiles.size() + " tài liệu")
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

            log.info("📤 [AUDIO] Gọi async method - Thread: {}", Thread.currentThread().getName());

            audioOverviewService.processAudioOverviewAsync(
                    savedAiSet.getId(), notebookId, userId, fileIds, voiceId, outputFormat, notes);

            result.put("aiSetId", savedAiSet.getId());
            result.put("status", "queued");
            result.put("success", true);
            result.put("message", "Audio Overview đang được tạo ở nền. Dùng aiSetId để theo dõi.");

        } catch (Exception e) {
            result.put("error", "Lỗi khi khởi tạo audio: " + e.getMessage());
            log.error("❌ [AUDIO] Error: {}", e.getMessage(), e);
        }

        return result;
    }

    // ================================
    // GET AUDIO BY AI SET ID
    // ================================

    @Transactional(readOnly = true)
    public AudioListResponse getAudioByAiSetId(UUID userId, UUID notebookId, UUID notebookAiSetId) {
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

        if (!"tts".equals(aiSet.getSetType())) {
            throw new BadRequestException("AI Set này không phải là audio/podcast");
        }

        List<TtsAsset> audios = ttsAssetRepository.findByAiSetId(notebookAiSetId);
        List<AudioResponse> audioResponses = audioMapper.toAudioResponseList(audios);

        return audioMapper.toAudioListResponse(aiSet, audioResponses);
    }
}
