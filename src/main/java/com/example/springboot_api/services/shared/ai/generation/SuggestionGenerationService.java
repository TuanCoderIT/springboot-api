package com.example.springboot_api.services.shared.ai.generation;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookAiSet;
import com.example.springboot_api.models.NotebookAiSetSuggestion;
import com.example.springboot_api.models.NotebookFile;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.shared.NotebookAiSetRepository;
import com.example.springboot_api.repositories.shared.NotebookAiSetSuggestionRepository;
import com.example.springboot_api.services.shared.ai.AIModelService;
import com.example.springboot_api.services.shared.ai.AiSetStatusService;
import com.example.springboot_api.services.shared.ai.DocumentSummarizationService;
import com.example.springboot_api.services.shared.ai.JsonParsingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service xử lý suggestion generation.
 * Tạo các câu hỏi gợi mở để người học suy ngẫm về nội dung.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SuggestionGenerationService {

    private final NotebookAiSetRepository aiSetRepository;
    private final NotebookAiSetSuggestionRepository suggestionRepository;
    private final AIModelService aiModelService;
    private final DocumentSummarizationService summarizationService;
    private final JsonParsingService jsonParsingService;
    private final AiSetStatusService statusService;

    /**
     * Xử lý suggestion generation ở background.
     */
    @Async
    @Transactional
    public void processSuggestionGenerationAsync(UUID aiSetId, UUID notebookId, UUID userId,
            List<UUID> fileIds, String additionalRequirements) {

        log.info("🚀 [SUGGESTION] Bắt đầu tạo suggestions - AiSet: {}", aiSetId);

        try {
            statusService.markProcessing(aiSetId);

            NotebookAiSet aiSet = aiSetRepository.findById(aiSetId).orElse(null);
            if (aiSet == null) {
                return;
            }

            Notebook notebook = aiSet.getNotebook();
            User user = aiSet.getCreatedBy();

            if (notebook == null || user == null) {
                statusService.markFailed(aiSetId, "Notebook/User không tồn tại");
                return;
            }

            List<NotebookFile> files = new ArrayList<>();
            aiSet.getNotebookAiSetFiles().forEach(asf -> {
                if (asf.getFile() != null) {
                    files.add(asf.getFile());
                }
            });

            if (files.isEmpty()) {
                statusService.markFailed(aiSetId, "Không có file");
                return;
            }

            // Tóm tắt documents
            String summaryText = summarizationService.summarizeDocuments(files, null);
            if (summaryText == null || summaryText.isEmpty()) {
                statusService.markFailed(aiSetId, "Không thể tóm tắt tài liệu");
                return;
            }

            // Tạo suggestions qua LLM
            String suggestionPrompt = buildSuggestionPrompt(summaryText, additionalRequirements);
            String llmResponse = aiModelService.callGeminiModel(suggestionPrompt);
            if (llmResponse == null || llmResponse.trim().isEmpty()) {
                statusService.markFailed(aiSetId, "LLM trả về response rỗng");
                return;
            }

            // Parse JSON
            List<Map<String, Object>> suggestions = jsonParsingService.parseSuggestionJson(llmResponse);
            if (suggestions == null || suggestions.isEmpty()) {
                statusService.markFailed(aiSetId, "Không thể parse suggestions");
                return;
            }

            // Lưu vào NotebookAiSetSuggestion entity
            String title = "Câu hỏi gợi mở";

            Map<String, Object> suggestionsData = new HashMap<>();
            suggestionsData.put("suggestions", suggestions);

            NotebookAiSetSuggestion suggestionEntity = NotebookAiSetSuggestion.builder()
                    .notebookAiSet(aiSet)
                    .suggestions(suggestionsData)
                    .createdBy(user)
                    .createdAt(OffsetDateTime.now())
                    .build();
            suggestionRepository.save(suggestionEntity);
            log.info("💾 [SUGGESTION] Đã lưu {} suggestions vào entity", suggestions.size());

            // Cập nhật AiSet title và status
            aiSet.setTitle(title);
            aiSet.setUpdatedAt(OffsetDateTime.now());
            aiSetRepository.save(aiSet);

            Map<String, Object> outputStats = new HashMap<>();
            outputStats.put("title", title);
            outputStats.put("suggestionCount", suggestions.size());
            outputStats.put("suggestionEntityId", suggestionEntity.getId().toString());
            statusService.markDone(aiSetId, outputStats);

            log.info("✅ [SUGGESTION] Hoàn thành - AiSet: {} | suggestions: {}", aiSetId, suggestions.size());

        } catch (Exception e) {
            statusService.markFailed(aiSetId, "Lỗi: " + e.getMessage());
            log.error("❌ [SUGGESTION] {}", e.getMessage(), e);
        }
    }

    /**
     * Tạo prompt cho suggestion generation.
     */
    public String buildSuggestionPrompt(String summaryText, String additionalRequirements) {
        String extraNote = (additionalRequirements != null && !additionalRequirements.isBlank())
                ? "\nYêu cầu thêm: " + additionalRequirements
                : "";

        return String.format(
                """
                        Bạn là chuyên gia giáo dục, tạo các câu hỏi gợi mở để kích thích tư duy.

                        Dựa trên nội dung sau, tạo các câu hỏi gợi mở giúp người học:
                        - Suy ngẫm sâu hơn về nội dung
                        - Liên hệ với kiến thức đã có
                        - Áp dụng vào thực tế

                        NỘI DUNG:
                        %s
                        %s

                        YÊU CẦU:
                        - Tạo 5-8 câu hỏi gợi mở
                        - Mỗi câu hỏi có: question, hint (gợi ý suy nghĩ), category (loại: comprehension/analysis/application/evaluation)
                        - Câu hỏi mở, không có đáp án đúng/sai cụ thể
                        - Khuyến khích tư duy phản biện

                        TRẢ VỀ JSON (KHÔNG có markdown wrapper):
                        {
                          "suggestions": [
                            {
                              "question": "Câu hỏi gợi mở?",
                              "hint": "Gợi ý hướng suy nghĩ",
                              "category": "analysis"
                            }
                          ]
                        }

                        CHỈ TRẢ VỀ JSON, KHÔNG CÓ TEXT KHÁC.
                        """,
                summaryText, extraNote);
    }
}
