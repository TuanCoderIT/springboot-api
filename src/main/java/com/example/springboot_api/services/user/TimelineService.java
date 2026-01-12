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
import com.example.springboot_api.dto.user.timeline.TimelineResponse;
import com.example.springboot_api.mappers.TimelineMapper;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookAiSet;
import com.example.springboot_api.models.NotebookAiSetFile;
import com.example.springboot_api.models.NotebookFile;
import com.example.springboot_api.models.TimelineEvent;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.admin.NotebookRepository;
import com.example.springboot_api.repositories.admin.UserRepository;
import com.example.springboot_api.repositories.shared.NotebookAiSetFileRepository;
import com.example.springboot_api.repositories.shared.NotebookAiSetRepository;
import com.example.springboot_api.repositories.shared.NotebookFileRepository;
import com.example.springboot_api.repositories.shared.TimelineEventRepository;
import com.example.springboot_api.services.shared.ai.generation.TimelineGenerationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service xử lý Timeline operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TimelineService {

    private final NotebookAiSetRepository aiSetRepository;
    private final NotebookAiSetFileRepository aiSetFileRepository;
    private final NotebookRepository notebookRepository;
    private final NotebookFileRepository notebookFileRepository;
    private final UserRepository userRepository;
    private final TimelineEventRepository timelineEventRepository;
    private final TimelineGenerationService timelineGenerationService;
    private final TimelineMapper timelineMapper;

    /**
     * Tạo timeline từ các files (async).
     * 
     * @return Map chứa aiSetId để track progress
     */
    public Map<String, Object> generateTimeline(UUID notebookId, UUID userId, List<UUID> fileIds,
            String mode, Integer maxEvents, String additionalRequirements) {

        Map<String, Object> result = new HashMap<>();

        try {
            Notebook notebook = notebookRepository.findById(notebookId)
                    .orElseThrow(() -> new NotFoundException("Notebook không tồn tại: " + notebookId));

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User không tồn tại: " + userId));

            if (fileIds == null || fileIds.isEmpty()) {
                throw new BadRequestException("Danh sách file IDs không được để trống");
            }

            // Validate files
            List<NotebookFile> selectedFiles = new ArrayList<>();
            for (UUID fileId : fileIds) {
                NotebookFile file = notebookFileRepository.findById(fileId).orElse(null);
                if (file != null && file.getNotebook() != null
                        && file.getNotebook().getId().equals(notebookId)) {
                    selectedFiles.add(file);
                }
            }

            if (selectedFiles.isEmpty()) {
                throw new BadRequestException("Không tìm thấy file hợp lệ nào");
            }

            // Defaults
            String modeValue = (mode != null && !mode.isBlank()) ? mode : "logic";
            int maxEventsValue = (maxEvents != null && maxEvents > 0) ? maxEvents : 25;

            // Tạo AI Set (trong transaction riêng)
            NotebookAiSet savedAiSet = createTimelineAiSet(notebook, user, selectedFiles, fileIds,
                    modeValue, maxEventsValue, additionalRequirements);

            result.put("aiSetId", savedAiSet.getId());
            result.put("status", "queued");
            result.put("success", true);
            result.put("message", "Timeline đang được tạo. Dùng aiSetId để theo dõi.");

            // Gọi async processing - PHẢI NGOÀI transaction
            log.info("📤 [TIMELINE] Gọi async method - Thread: {}", Thread.currentThread().getName());
            timelineGenerationService.processTimelineGenerationAsync(
                    savedAiSet.getId(), notebookId, userId, fileIds,
                    modeValue, maxEventsValue, additionalRequirements);

        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("success", false);
            log.error("❌ [TIMELINE] Error: {}", e.getMessage(), e);
        }

        return result;
    }

    /**
     * Tạo NotebookAiSet cho timeline và liên kết files.
     */
    @Transactional
    public NotebookAiSet createTimelineAiSet(Notebook notebook, User user, List<NotebookFile> selectedFiles,
            List<UUID> fileIds, String mode, int maxEvents, String additionalRequirements) {

        OffsetDateTime now = OffsetDateTime.now();
        Map<String, Object> inputConfig = new HashMap<>();
        inputConfig.put("fileIds", fileIds);
        inputConfig.put("mode", mode);
        inputConfig.put("maxEvents", maxEvents);
        if (additionalRequirements != null && !additionalRequirements.isBlank()) {
            inputConfig.put("additionalRequirements", additionalRequirements);
        }

        NotebookAiSet aiSet = NotebookAiSet.builder()
                .notebook(notebook)
                .createdBy(user)
                .setType("timeline")
                .status("queued")
                .title("Timeline từ " + selectedFiles.size() + " tài liệu")
                .inputConfig(inputConfig)
                .createdAt(now)
                .updatedAt(now)
                .build();
        NotebookAiSet savedAiSet = aiSetRepository.save(aiSet);

        // Lưu file links
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

    /**
     * Lấy timeline theo AI Set ID.
     */
    @Transactional(readOnly = true)
    public TimelineResponse getTimelineByAiSetId(UUID userId, UUID notebookId, UUID aiSetId) {
        NotebookAiSet aiSet = aiSetRepository.findById(aiSetId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy AI Set: " + aiSetId));

        if (aiSet.getNotebook() == null || !aiSet.getNotebook().getId().equals(notebookId)) {
            throw new BadRequestException("AI Set không thuộc notebook này");
        }

        if (!"timeline".equals(aiSet.getSetType())) {
            throw new BadRequestException("AI Set này không phải là timeline");
        }

        List<TimelineEvent> events = timelineEventRepository.findByAiSetIdOrderByEventOrder(aiSetId);

        return timelineMapper.toTimelineResponse(aiSet, events);
    }
}
