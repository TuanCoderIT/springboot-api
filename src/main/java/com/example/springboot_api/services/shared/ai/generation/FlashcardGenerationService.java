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

import com.example.springboot_api.models.Flashcard;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookAiSet;
import com.example.springboot_api.models.NotebookFile;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.shared.FlashcardRepository;
import com.example.springboot_api.repositories.shared.NotebookAiSetRepository;
import com.example.springboot_api.services.shared.ai.AIModelService;
import com.example.springboot_api.services.shared.ai.AiSetStatusService;
import com.example.springboot_api.services.shared.ai.DocumentSummarizationService;
import com.example.springboot_api.services.shared.ai.JsonParsingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service xử lý flashcard generation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FlashcardGenerationService {

    private final NotebookAiSetRepository aiSetRepository;
    private final FlashcardRepository flashcardRepository;
    private final AIModelService aiModelService;
    private final DocumentSummarizationService summarizationService;
    private final JsonParsingService jsonParsingService;
    private final AiSetStatusService statusService;

    /**
     * Xử lý flashcard generation ở background (async).
     */
    @Async
    @Transactional
    public void processFlashcardGenerationAsync(UUID aiSetId, UUID notebookId, UUID userId,
            List<UUID> fileIds, String numberOfCards, String additionalRequirements) {

        log.info("🚀 [FLASHCARD] Bắt đầu tạo flashcards - AiSet: {} | Thread: {}", aiSetId,
                Thread.currentThread().getName());

        try {
            statusService.markProcessing(aiSetId);

            NotebookAiSet aiSet = aiSetRepository.findById(aiSetId).orElse(null);
            if (aiSet == null) {
                log.error("❌ [FLASHCARD] Không tìm thấy AiSet: {}", aiSetId);
                return;
            }

            Notebook notebook = aiSet.getNotebook();
            User user = aiSet.getCreatedBy();

            if (notebook == null || user == null) {
                statusService.markFailed(aiSetId, "Không tìm thấy notebook hoặc user từ AiSet");
                return;
            }

            List<NotebookFile> selectedFiles = new ArrayList<>();
            aiSet.getNotebookAiSetFiles().forEach(asf -> {
                if (asf.getFile() != null) {
                    selectedFiles.add(asf.getFile());
                }
            });

            if (selectedFiles.isEmpty()) {
                statusService.markFailed(aiSetId, "Không tìm thấy file nào từ AiSet");
                return;
            }

            log.info("📄 [FLASHCARD] Đang tóm tắt tài liệu...");
            String summaryText = summarizationService.summarizeDocuments(selectedFiles, null);
            if (summaryText == null || summaryText.isEmpty()) {
                statusService.markFailed(aiSetId, "Không thể tóm tắt tài liệu");
                return;
            }

            String flashcardPrompt = buildFlashcardPrompt(summaryText, numberOfCards, additionalRequirements);

            log.info("🤖 [FLASHCARD] Đang gọi LLM...");
            String llmResponse = aiModelService.callGeminiModel(flashcardPrompt);
            if (llmResponse == null || llmResponse.trim().isEmpty()) {
                statusService.markFailed(aiSetId, "LLM trả về response rỗng");
                return;
            }

            List<Map<String, Object>> flashcards = jsonParsingService.parseJsonArray(llmResponse);
            if (flashcards == null || flashcards.isEmpty()) {
                statusService.markFailed(aiSetId, "Không thể parse flashcards từ LLM response");
                return;
            }

            List<UUID> savedCardIds = saveFlashcardsToDatabase(notebook, user, aiSet, flashcards);

            Map<String, Object> outputStats = new HashMap<>();
            outputStats.put("flashcardIds", savedCardIds);
            outputStats.put("flashcardCount", savedCardIds.size());
            statusService.markDone(aiSetId, outputStats);

            log.info("✅ [FLASHCARD] Hoàn thành - AiSet: {} | Số flashcards: {}", aiSetId, savedCardIds.size());

        } catch (Exception e) {
            String errorMsg = "Lỗi khi tạo flashcards: " + e.getMessage();
            statusService.markFailed(aiSetId, errorMsg);
            log.error("❌ [FLASHCARD] {}", errorMsg, e);
        }
    }

    /**
     * Tạo prompt cho flashcard generation.
     */
    public String buildFlashcardPrompt(String summaryText, String numberOfCards, String additionalRequirements) {
        String additionalSection = "";
        if (additionalRequirements != null && !additionalRequirements.trim().isEmpty()) {
            additionalSection = String.format("""

                    ---
                    YÊU CẦU BỔ SUNG TỪ NGƯỜI DÙNG:

                    %s

                    (Hãy ưu tiên tuân thủ yêu cầu bổ sung này khi tạo flashcards)
                    """, additionalRequirements.trim());
        }

        return String.format("""
                Bạn là chuyên gia tạo flashcard học tập ngắn gọn, dễ nhớ.

                Dưới đây là phần nội dung đã được tóm tắt từ nhiều tài liệu trong notebook.
                Hãy tạo bộ flashcard bám sát nội dung, chú trọng tính súc tích, dễ ôn tập.

                ---
                NỘI DUNG TÓM TẮT:

                %s

                ---%s

                Mục tiêu:
                - Số lượng flashcard: %s (few = 5-8, standard = 10-15, many = 16-25)
                - Front: câu hỏi/khái niệm/ngắn gọn.
                - Back: giải thích ngắn, chính xác; có thể kèm bước, công thức, bullet ngắn.
                - Có thể kèm hint và example nếu hữu ích cho ghi nhớ.

                Format JSON response:
                [
                  {
                    "front_text": "Thuật ngữ hay câu hỏi ngắn",
                    "back_text": "Giải thích súc tích, dễ nhớ",
                    "hint": "Gợi ý (optional)",
                    "example": "Ví dụ minh họa ngắn (optional)",
                    "image_url": null,
                    "audio_url": null,
                    "extra_metadata": {"tags": ["topic1", "topic2"]}
                  }
                ]

                CHỈ TRẢ VỀ JSON ARRAY, KHÔNG CÓ TEXT KHÁC.
                """, summaryText, additionalSection, numberOfCards);
    }

    /**
     * Lưu flashcards vào database.
     */
    @Transactional
    public List<UUID> saveFlashcardsToDatabase(Notebook notebook, User user, NotebookAiSet aiSet,
            List<Map<String, Object>> flashcards) {
        List<UUID> savedIds = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();

        for (Map<String, Object> cardData : flashcards) {
            String frontText = (String) (cardData.get("front_text") != null ? cardData.get("front_text")
                    : cardData.get("frontText"));
            String backText = (String) (cardData.get("back_text") != null ? cardData.get("back_text")
                    : cardData.get("backText"));
            if (frontText == null || frontText.isBlank() || backText == null || backText.isBlank()) {
                continue;
            }

            String hint = (String) cardData.get("hint");
            String example = (String) cardData.get("example");
            String imageUrl = (String) (cardData.get("image_url") != null ? cardData.get("image_url")
                    : cardData.get("imageUrl"));
            String audioUrl = (String) (cardData.get("audio_url") != null ? cardData.get("audio_url")
                    : cardData.get("audioUrl"));

            @SuppressWarnings("unchecked")
            Map<String, Object> extraMetadata = (Map<String, Object>) cardData.get("extra_metadata");
            if (extraMetadata == null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> camelMeta = (Map<String, Object>) cardData.get("extraMetadata");
                extraMetadata = camelMeta;
            }

            Flashcard flashcard = Flashcard.builder()
                    .notebook(notebook)
                    .createdBy(user)
                    .notebookAiSets(aiSet)
                    .frontText(frontText.trim())
                    .backText(backText.trim())
                    .hint(hint != null ? hint.trim() : null)
                    .example(example != null ? example.trim() : null)
                    .imageUrl(imageUrl != null ? imageUrl.trim() : null)
                    .audioUrl(audioUrl != null ? audioUrl.trim() : null)
                    .extraMetadata(extraMetadata)
                    .createdAt(now)
                    .build();
            Flashcard saved = flashcardRepository.save(flashcard);
            savedIds.add(saved.getId());
        }

        return savedIds;
    }
}
