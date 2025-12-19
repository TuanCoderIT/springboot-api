package com.example.springboot_api.services.user;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot_api.common.exceptions.BadRequestException;
import com.example.springboot_api.common.exceptions.NotFoundException;
import com.example.springboot_api.dto.user.ai.MindmapResponse;
import com.example.springboot_api.mappers.MindmapMapper;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookAiSet;
import com.example.springboot_api.models.NotebookAiSetFile;
import com.example.springboot_api.models.NotebookFile;
import com.example.springboot_api.models.NotebookMindmap;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.admin.NotebookMemberRepository;
import com.example.springboot_api.repositories.admin.NotebookRepository;
import com.example.springboot_api.repositories.admin.UserRepository;
import com.example.springboot_api.repositories.shared.MindmapRepository;
import com.example.springboot_api.repositories.shared.NotebookAiSetFileRepository;
import com.example.springboot_api.repositories.shared.NotebookAiSetRepository;
import com.example.springboot_api.repositories.shared.NotebookFileRepository;
import com.example.springboot_api.services.shared.ai.generation.MindmapGenerationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service xử lý các tính năng liên quan đến Mindmap.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MindmapService {

    private final MindmapRepository mindmapRepository;
    private final NotebookAiSetRepository aiSetRepository;
    private final NotebookAiSetFileRepository aiSetFileRepository;
    private final NotebookMemberRepository memberRepository;
    private final NotebookRepository notebookRepository;
    private final NotebookFileRepository notebookFileRepository;
    private final UserRepository userRepository;
    private final MindmapMapper mindmapMapper;
    private final MindmapGenerationService mindmapGenerationService;

    // ================================
    // GENERATE MINDMAP (ASYNC)
    // ================================

    public Map<String, Object> generateMindmap(UUID notebookId, UUID userId, List<UUID> fileIds,
            String additionalRequirements) {
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

            NotebookAiSet savedAiSet = createMindmapAiSet(notebook, user, selectedFiles, fileIds,
                    additionalRequirements);

            result.put("aiSetId", savedAiSet.getId());
            result.put("status", "queued");
            result.put("message", "Mindmap đang được tạo ở nền. Sử dụng aiSetId để theo dõi tiến trình.");
            result.put("success", true);

            log.info("📤 [MINDMAP] Gọi async method - Thread: {}", Thread.currentThread().getName());

            mindmapGenerationService.processMindmapGenerationAsync(
                    savedAiSet.getId(), notebookId, userId, fileIds, additionalRequirements);

        } catch (Exception e) {
            result.put("error", "Lỗi khi khởi tạo mindmap: " + e.getMessage());
            log.error("❌ [MINDMAP] Error: {}", e.getMessage(), e);
        }

        return result;
    }

    @Transactional
    public NotebookAiSet createMindmapAiSet(Notebook notebook, User user, List<NotebookFile> selectedFiles,
            List<UUID> fileIds, String additionalRequirements) {

        OffsetDateTime now = OffsetDateTime.now();
        Map<String, Object> inputConfig = new HashMap<>();
        inputConfig.put("fileIds", fileIds);
        if (additionalRequirements != null && !additionalRequirements.trim().isEmpty()) {
            inputConfig.put("additionalRequirements", additionalRequirements.trim());
        }

        NotebookAiSet aiSet = NotebookAiSet.builder()
                .notebook(notebook)
                .createdBy(user)
                .setType("mindmap")
                .status("queued")
                .title("Mindmap từ " + selectedFiles.size() + " tài liệu")
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
    // GET MINDMAP BY AI SET ID
    // ================================

    @Transactional(readOnly = true)
    public MindmapResponse getMindmapByAiSetId(UUID userId, UUID notebookId, UUID aiSetId) {
        validateMembership(userId, notebookId);

        NotebookAiSet aiSet = aiSetRepository.findById(aiSetId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy AI Set với ID: " + aiSetId));

        if (!aiSet.getNotebook().getId().equals(notebookId)) {
            throw new BadRequestException("AI Set không thuộc về notebook này.");
        }

        List<NotebookMindmap> mindmaps = mindmapRepository.findByAiSetId(aiSetId);
        if (mindmaps.isEmpty()) {
            throw new NotFoundException("Không tìm thấy mindmap cho AI Set này.");
        }

        return mindmapMapper.toMindmapResponse(mindmaps.get(0));
    }

    @Transactional(readOnly = true)
    public List<MindmapResponse> getMindmapsByNotebookId(UUID userId, UUID notebookId) {
        validateMembership(userId, notebookId);

        List<NotebookMindmap> mindmaps = mindmapRepository.findByNotebookId(notebookId);
        return mindmaps.stream()
                .map(mindmapMapper::toMindmapResponse)
                .collect(Collectors.toList());
    }

    private void validateMembership(UUID userId, UUID notebookId) {
        var member = memberRepository.findByNotebookIdAndUserId(notebookId, userId)
                .orElseThrow(() -> new BadRequestException("Bạn không phải thành viên của notebook này."));

        if (!"approved".equals(member.getStatus())) {
            throw new BadRequestException("Bạn chưa được duyệt tham gia notebook này.");
        }
    }
}
