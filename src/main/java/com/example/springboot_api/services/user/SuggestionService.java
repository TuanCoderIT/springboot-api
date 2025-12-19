package com.example.springboot_api.services.user;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot_api.common.exceptions.NotFoundException;
import com.example.springboot_api.dto.user.suggestion.SuggestionResponse;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookAiSet;
import com.example.springboot_api.models.NotebookAiSetFile;
import com.example.springboot_api.models.NotebookAiSetSuggestion;
import com.example.springboot_api.models.NotebookFile;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.admin.NotebookRepository;
import com.example.springboot_api.repositories.admin.UserRepository;
import com.example.springboot_api.repositories.shared.NotebookAiSetFileRepository;
import com.example.springboot_api.repositories.shared.NotebookAiSetRepository;
import com.example.springboot_api.repositories.shared.NotebookAiSetSuggestionRepository;
import com.example.springboot_api.repositories.shared.NotebookFileRepository;
import com.example.springboot_api.services.shared.ai.generation.SuggestionGenerationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SuggestionService {

    private final NotebookAiSetSuggestionRepository suggestionRepository;
    private final NotebookRepository notebookRepository;
    private final UserRepository userRepository;
    private final NotebookAiSetRepository aiSetRepository;
    private final NotebookAiSetFileRepository aiSetFileRepository;
    private final NotebookFileRepository notebookFileRepository;
    private final SuggestionGenerationService suggestionGenerationService;

    // ================================
    // GENERATE SUGGESTIONS (ASYNC)
    // ================================

    public Map<String, Object> generateSuggestions(UUID notebookId, UUID userId, List<UUID> fileIds,
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

            NotebookAiSet savedAiSet = createSuggestionAiSet(notebook, user, selectedFiles, fileIds);

            result.put("aiSetId", savedAiSet.getId());
            result.put("status", "queued");
            result.put("message", "Suggestions đang được tạo ở nền. Sử dụng aiSetId để theo dõi tiến trình.");
            result.put("success", true);

            log.info("📤 [SUGGESTION] Gọi async method - Thread: {}", Thread.currentThread().getName());

            suggestionGenerationService.processSuggestionGenerationAsync(
                    savedAiSet.getId(), notebookId, userId, fileIds, additionalRequirements);

        } catch (Exception e) {
            result.put("error", "Lỗi khi khởi tạo suggestions: " + e.getMessage());
            log.error("❌ [SUGGESTION] Error: {}", e.getMessage(), e);
        }

        return result;
    }

    @Transactional
    public NotebookAiSet createSuggestionAiSet(Notebook notebook, User user, List<NotebookFile> selectedFiles,
            List<UUID> fileIds) {

        OffsetDateTime now = OffsetDateTime.now();
        Map<String, Object> inputConfig = new HashMap<>();
        inputConfig.put("fileIds", fileIds);

        NotebookAiSet aiSet = NotebookAiSet.builder()
                .notebook(notebook)
                .createdBy(user)
                .setType("suggestion")
                .status("queued")
                .title("Câu hỏi gợi mở từ " + selectedFiles.size() + " tài liệu")
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
    // GET SUGGESTIONS BY AI SET ID
    // ================================

    public SuggestionResponse getSuggestionsByAiSetId(UUID userId, UUID notebookId, UUID aiSetId) {
        if (!notebookRepository.existsById(notebookId)) {
            throw new NotFoundException("Notebook không tồn tại");
        }
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User không tồn tại");
        }

        NotebookAiSetSuggestion suggestion = suggestionRepository.findByNotebookAiSetId(aiSetId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy suggestions cho AI Set ID: " + aiSetId));

        Map<String, Object> data = suggestion.getSuggestions();
        @SuppressWarnings("unchecked")
        List<String> list = (List<String>) data.get("suggestions");

        return SuggestionResponse.builder()
                .aiSetId(aiSetId)
                .suggestions(list)
                .build();
    }
}
