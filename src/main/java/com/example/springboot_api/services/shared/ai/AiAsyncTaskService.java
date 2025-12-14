package com.example.springboot_api.services.shared.ai;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.springboot_api.models.Flashcard;
import com.example.springboot_api.models.LlmModel;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookAiSet;
import com.example.springboot_api.models.NotebookFile;
import com.example.springboot_api.models.NotebookQuizOption;
import com.example.springboot_api.models.NotebookQuizz;
import com.example.springboot_api.models.TtsAsset;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.shared.FileChunkRepository;
import com.example.springboot_api.repositories.shared.FlashcardRepository;
import com.example.springboot_api.repositories.shared.NotebookAiSetRepository;
import com.example.springboot_api.repositories.shared.QuizOptionRepository;
import com.example.springboot_api.repositories.shared.QuizRepository;
import com.example.springboot_api.repositories.shared.TtsAssetRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;

/**
 * Service xử lý các tác vụ AI chạy nền (async).
 * Tách riêng để đảm bảo @Async hoạt động (tránh self-invocation problem).
 * 
 * Sử dụng NotebookAiSet để quản lý các AI generation sets.
 * Mỗi quiz/flashcard/tts/video sẽ có foreign key tới NotebookAiSet.
 */
@Service
@RequiredArgsConstructor
public class AiAsyncTaskService {

    private final NotebookAiSetRepository aiSetRepository;
    private final FileChunkRepository fileChunkRepository;
    private final QuizRepository quizRepository;
    private final QuizOptionRepository quizOptionRepository;
    private final FlashcardRepository flashcardRepository;
    private final AIModelService aiModelService;
    private final TtsAssetRepository ttsAssetRepository;
    private final com.example.springboot_api.repositories.shared.VideoAssetRepository videoAssetRepository;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;
    private final VideoFrameService videoFrameService;
    private final com.example.springboot_api.repositories.admin.NotebookRepository notebookRepository;
    private final com.example.springboot_api.repositories.admin.UserRepository userRepository;
    private final com.example.springboot_api.repositories.shared.NotebookFileRepository notebookFileRepository;

    @Value("${google.api.gemini_key:}")
    private String geminiApiKeyConfig;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    /**
     * Xử lý quiz generation ở background (async).
     * Method này PHẢI được gọi từ một bean KHÁC để @Async hoạt động.
     * 
     * QUAN TRỌNG: Nhận IDs thay vì managed entities để tránh
     * LazyInitializationException.
     * 
     * @param aiSetId                ID của NotebookAiSet đã tạo
     * @param notebookId             Notebook ID
     * @param userId                 User ID
     * @param fileIds                Danh sách file IDs
     * @param numberOfQuestions      Số lượng câu hỏi
     * @param difficultyLevel        Độ khó
     * @param additionalRequirements Yêu cầu bổ sung
     */
    @Async
    @Transactional
    public void processQuizGenerationAsync(UUID aiSetId, UUID notebookId, UUID userId,
            List<UUID> fileIds, String numberOfQuestions, String difficultyLevel,
            String additionalRequirements) {

        System.out.println(
                "🚀 [ASYNC] Bắt đầu tạo quiz - AiSet: " + aiSetId + " | Thread: " + Thread.currentThread().getName());

        try {
            // Cập nhật status thành processing
            updateAiSetStatus(aiSetId, "processing", null, null);

            // Load AI Set với các thông tin liên quan
            NotebookAiSet aiSet = aiSetRepository.findById(aiSetId).orElse(null);
            if (aiSet == null) {
                System.err.println("❌ [ASYNC] Không tìm thấy AiSet: " + aiSetId);
                return;
            }

            Notebook notebook = aiSet.getNotebook();
            User user = aiSet.getCreatedBy();

            if (notebook == null || user == null) {
                String errorMsg = "Không tìm thấy notebook hoặc user từ AiSet";
                updateAiSetStatus(aiSetId, "failed", errorMsg, null);
                System.err.println("❌ [ASYNC] " + errorMsg);
                return;
            }

            // Load files từ AiSetFiles
            List<NotebookFile> selectedFiles = new ArrayList<>();
            aiSet.getNotebookAiSetFiles().forEach(asf -> {
                if (asf.getFile() != null) {
                    selectedFiles.add(asf.getFile());
                }
            });

            if (selectedFiles.isEmpty()) {
                String errorMsg = "Không tìm thấy file nào từ AiSet";
                updateAiSetStatus(aiSetId, "failed", errorMsg, null);
                System.err.println("❌ [ASYNC] " + errorMsg);
                return;
            }

            // Tóm tắt documents
            System.out.println("📄 [ASYNC] Đang tóm tắt tài liệu...");
            String summaryText = summarizeDocuments(selectedFiles, null);
            if (summaryText == null || summaryText.isEmpty()) {
                String errorMsg = "Không thể tóm tắt tài liệu (có thể không có chunks)";
                updateAiSetStatus(aiSetId, "failed", errorMsg, null);
                System.err.println("❌ [ASYNC] " + errorMsg);
                return;
            }

            // Tạo prompt cho quiz
            String quizPrompt = buildQuizPrompt(summaryText, numberOfQuestions, difficultyLevel,
                    additionalRequirements);

            // Gọi LLM để tạo quiz
            System.out.println("🤖 [ASYNC] Đang gọi LLM...");
            String llmResponse = aiModelService.callGeminiModel(quizPrompt);
            if (llmResponse == null || llmResponse.trim().isEmpty()) {
                String errorMsg = "LLM trả về response rỗng";
                updateAiSetStatus(aiSetId, "failed", errorMsg, null);
                System.err.println("❌ [ASYNC] " + errorMsg);
                return;
            }

            // Parse JSON và lưu quiz vào database
            System.out.println("💾 [ASYNC] Đang lưu quiz vào database...");
            List<Map<String, Object>> quizList = parseQuizJsonResponse(llmResponse);
            if (quizList == null || quizList.isEmpty()) {
                String errorMsg = "Không thể parse quiz từ LLM response";
                updateAiSetStatus(aiSetId, "failed", errorMsg, null);
                System.err.println("❌ [ASYNC] " + errorMsg);
                return;
            }

            // Lưu quiz vào database VỚI foreign key tới AiSet
            List<UUID> savedQuizIds = saveQuizzesToDatabase(notebook, user, aiSet, quizList);

            // Cập nhật AiSet thành công
            Map<String, Object> outputStats = new HashMap<>();
            outputStats.put("quizIds", savedQuizIds);
            outputStats.put("quizCount", savedQuizIds.size());
            updateAiSetStatus(aiSetId, "done", null, outputStats);

            System.out.println(
                    "✅ [ASYNC] Hoàn thành tạo quiz - AiSet: " + aiSetId + " | Số quiz: " + savedQuizIds.size());

        } catch (Exception e) {
            String errorMsg = "Lỗi khi tạo quiz: " + e.getMessage();
            updateAiSetStatus(aiSetId, "failed", errorMsg, null);
            System.err.println("❌ [ASYNC] " + errorMsg);
            e.printStackTrace();
        }
    }

    // ================================
    // AUDIO OVERVIEW ASYNC (delay trước khi gọi LLM)
    // ================================
    @Async
    @Transactional
    public void processAudioOverviewAsync(UUID aiSetId, UUID notebookId, UUID userId, List<UUID> fileIds,
            String voiceId, String outputFormat, String notes) {

        System.out.println("🚀 [ASYNC] Bắt đầu tạo Audio Overview - AiSet: " + aiSetId);

        try {
            updateAiSetStatus(aiSetId, "processing", null, null);

            NotebookAiSet aiSet = aiSetRepository.findById(aiSetId).orElse(null);
            if (aiSet == null) {
                System.err.println("❌ [ASYNC] Không tìm thấy AiSet: " + aiSetId);
                return;
            }
            Notebook notebook = aiSet.getNotebook();
            User user = aiSet.getCreatedBy();
            if (notebook == null || user == null) {
                String errorMsg = "Không tìm thấy notebook hoặc user từ AiSet";
                updateAiSetStatus(aiSetId, "failed", errorMsg, null);
                System.err.println("❌ [ASYNC] " + errorMsg);
                return;
            }

            // Load files từ AiSetFiles
            List<NotebookFile> selectedFiles = new ArrayList<>();
            aiSet.getNotebookAiSetFiles().forEach(asf -> {
                if (asf.getFile() != null) {
                    selectedFiles.add(asf.getFile());
                }
            });
            if (selectedFiles.isEmpty()) {
                String errorMsg = "Không tìm thấy file nào từ AiSet";
                updateAiSetStatus(aiSetId, "failed", errorMsg, null);
                System.err.println("❌ [ASYNC] " + errorMsg);
                return;
            }

            // Delay đã được xử lý trong summarizeDocuments (giữa các file/chunk)
            // Không cần delay cố định 60s ở đây nữa

            // Sinh JSON script overview (có validate JSON)
            String json = generateAudioOverviewJson(selectedFiles, null);
            ObjectNode node = objectMapper.readValue(json, ObjectNode.class);
            String script = node.path("voice_script_overview").asText();
            if (script == null || script.isBlank()) {
                String errorMsg = "voice_script_overview trống.";
                updateAiSetStatus(aiSetId, "failed", errorMsg, null);
                System.err.println("❌ [ASYNC] " + errorMsg);
                return;
            }

            // Gọi ElevenLabs và lưu asset (gắn aiSet)
            TtsAsset asset = generateAudioOverviewAsset(
                    script, voiceId, outputFormat, notebook, user, aiSet);

            Map<String, Object> outputStats = new HashMap<>();
            outputStats.put("audioUrl", asset.getAudioUrl());
            outputStats.put("voiceName", asset.getVoiceName());

            updateAiSetStatus(aiSetId, "done", null, outputStats);
            System.out.println("✅ [ASYNC] Hoàn thành Audio Overview - AiSet: " + aiSetId);

        } catch (Exception e) {
            String errorMsg = "Lỗi khi tạo Audio Overview: " + e.getMessage();
            updateAiSetStatus(aiSetId, "failed", errorMsg, null);
            System.err.println("❌ [ASYNC] " + errorMsg);
            e.printStackTrace();
        }
    }

    /**
     * Xử lý flashcard generation ở background (async).
     * Nhận IDs để tránh LazyInitializationException.
     */
    @Async
    @Transactional
    public void processFlashcardGenerationAsync(UUID aiSetId, UUID notebookId, UUID userId,
            List<UUID> fileIds, String numberOfCards, String additionalRequirements) {

        System.out.println("🚀 [ASYNC] Bắt đầu tạo flashcards - AiSet: " + aiSetId + " | Thread: "
                + Thread.currentThread().getName());

        try {
            updateAiSetStatus(aiSetId, "processing", null, null);

            NotebookAiSet aiSet = aiSetRepository.findById(aiSetId).orElse(null);
            if (aiSet == null) {
                System.err.println("❌ [ASYNC] Không tìm thấy AiSet: " + aiSetId);
                return;
            }

            Notebook notebook = aiSet.getNotebook();
            User user = aiSet.getCreatedBy();

            if (notebook == null || user == null) {
                String errorMsg = "Không tìm thấy notebook hoặc user từ AiSet";
                updateAiSetStatus(aiSetId, "failed", errorMsg, null);
                System.err.println("❌ [ASYNC] " + errorMsg);
                return;
            }

            List<NotebookFile> selectedFiles = new ArrayList<>();
            aiSet.getNotebookAiSetFiles().forEach(asf -> {
                if (asf.getFile() != null) {
                    selectedFiles.add(asf.getFile());
                }
            });

            if (selectedFiles.isEmpty()) {
                String errorMsg = "Không tìm thấy file nào từ AiSet";
                updateAiSetStatus(aiSetId, "failed", errorMsg, null);
                System.err.println("❌ [ASYNC] " + errorMsg);
                return;
            }

            System.out.println("📄 [ASYNC] Đang tóm tắt tài liệu cho flashcards...");
            String summaryText = summarizeDocuments(selectedFiles, null);
            if (summaryText == null || summaryText.isEmpty()) {
                String errorMsg = "Không thể tóm tắt tài liệu (có thể không có chunks)";
                updateAiSetStatus(aiSetId, "failed", errorMsg, null);
                System.err.println("❌ [ASYNC] " + errorMsg);
                return;
            }

            String flashcardPrompt = buildFlashcardPrompt(summaryText, numberOfCards, additionalRequirements);

            System.out.println("🤖 [ASYNC] Đang gọi LLM tạo flashcards...");
            String llmResponse = aiModelService.callGeminiModel(flashcardPrompt);
            if (llmResponse == null || llmResponse.trim().isEmpty()) {
                String errorMsg = "LLM trả về response rỗng";
                updateAiSetStatus(aiSetId, "failed", errorMsg, null);
                System.err.println("❌ [ASYNC] " + errorMsg);
                return;
            }

            List<Map<String, Object>> flashcards = parseFlashcardJsonResponse(llmResponse);
            if (flashcards == null || flashcards.isEmpty()) {
                String errorMsg = "Không thể parse flashcards từ LLM response";
                updateAiSetStatus(aiSetId, "failed", errorMsg, null);
                System.err.println("❌ [ASYNC] " + errorMsg);
                return;
            }

            List<UUID> savedCardIds = saveFlashcardsToDatabase(notebook, user, aiSet, flashcards);

            Map<String, Object> outputStats = new HashMap<>();
            outputStats.put("flashcardIds", savedCardIds);
            outputStats.put("flashcardCount", savedCardIds.size());
            updateAiSetStatus(aiSetId, "done", null, outputStats);

            System.out.println("✅ [ASYNC] Hoàn thành tạo flashcards - AiSet: " + aiSetId + " | Số flashcards: "
                    + savedCardIds.size());

        } catch (Exception e) {
            String errorMsg = "Lỗi khi tạo flashcards: " + e.getMessage();
            updateAiSetStatus(aiSetId, "failed", errorMsg, null);
            System.err.println("❌ [ASYNC] " + errorMsg);
            e.printStackTrace();
        }
    }

    /**
     * Cập nhật status của NotebookAiSet.
     */
    @Transactional
    public void updateAiSetStatus(UUID aiSetId, String status, String errorMessage, Map<String, Object> outputStats) {
        aiSetRepository.findById(aiSetId).ifPresent(aiSet -> {
            aiSet.setStatus(status);
            aiSet.setErrorMessage(errorMessage);
            aiSet.setUpdatedAt(OffsetDateTime.now());

            if ("processing".equals(status)) {
                aiSet.setStartedAt(OffsetDateTime.now());
            }
            if ("done".equals(status) || "failed".equals(status)) {
                aiSet.setFinishedAt(OffsetDateTime.now());
            }
            if (outputStats != null) {
                aiSet.setOutputStats(outputStats);
            }
            aiSetRepository.save(aiSet);
        });
    }

    /**
     * Lưu quiz vào database với foreign key tới NotebookAiSet.
     */
    @Transactional
    public List<UUID> saveQuizzesToDatabase(Notebook notebook, User user,
            NotebookAiSet aiSet, List<Map<String, Object>> quizList) {

        List<UUID> savedQuizIds = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();

        for (Map<String, Object> quizData : quizList) {
            String question = (String) quizData.get("question");
            String explanation = (String) quizData.get("explanation");
            Object diffObj = quizData.get("difficulty_level");
            Short difficultyLevel = diffObj != null ? ((Number) diffObj).shortValue() : 2;

            NotebookQuizz quiz = NotebookQuizz.builder()
                    .notebook(notebook)
                    .question(question)
                    .explanation(explanation)
                    .difficultyLevel(difficultyLevel)
                    .createdBy(user)
                    .notebookAiSets(aiSet) // Liên kết quiz với AI Set
                    .createdAt(now)
                    .build();
            NotebookQuizz savedQuiz = quizRepository.save(quiz);
            savedQuizIds.add(savedQuiz.getId());

            // Lưu options
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> options = (List<Map<String, Object>>) quizData.get("options");
            if (options != null) {
                for (Map<String, Object> optionData : options) {
                    String text = (String) optionData.get("text");
                    Boolean isCorrect = optionData.get("is_correct") != null
                            ? (Boolean) optionData.get("is_correct")
                            : false;
                    String feedback = (String) optionData.get("feedback");
                    Object posObj = optionData.get("position");
                    Integer position = posObj != null ? ((Number) posObj).intValue() : 0;

                    NotebookQuizOption option = NotebookQuizOption.builder()
                            .quiz(savedQuiz)
                            .text(text)
                            .isCorrect(isCorrect)
                            .feedback(feedback)
                            .position(position)
                            .createdAt(now)
                            .build();
                    quizOptionRepository.save(option);
                }
            }
        }

        return savedQuizIds;
    }

    /**
     * Lưu flashcards vào database với foreign key tới NotebookAiSet.
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

    /**
     * Parse JSON response từ LLM thành list quiz.
     */
    public List<Map<String, Object>> parseQuizJsonResponse(String llmResponse) {
        try {
            String jsonString = extractJsonFromResponse(llmResponse);
            if (jsonString == null) {
                return null;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> quizList = objectMapper.readValue(jsonString, List.class);
            return quizList;
        } catch (Exception e) {
            System.err.println("❌ Lỗi parse quiz JSON: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Parse JSON response từ LLM thành list flashcards.
     */
    public List<Map<String, Object>> parseFlashcardJsonResponse(String llmResponse) {
        try {
            String jsonString = extractJsonFromResponse(llmResponse);
            if (jsonString == null) {
                return null;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> cardList = objectMapper.readValue(jsonString, List.class);
            return cardList;
        } catch (Exception e) {
            System.err.println("❌ Lỗi parse flashcard JSON: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Trích xuất JSON từ LLM response (có thể có markdown wrapper).
     */
    private String extractJsonFromResponse(String response) {
        if (response == null || response.isBlank()) {
            return null;
        }

        String trimmed = response.trim();

        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            return trimmed;
        }

        // Tìm trong code block ```json ... ```
        Pattern codeBlockPattern = Pattern.compile("```(?:json)?\\s*\\n?([\\s\\S]*?)```", Pattern.MULTILINE);
        Matcher matcher = codeBlockPattern.matcher(trimmed);
        if (matcher.find()) {
            String content = matcher.group(1).trim();
            if (content.startsWith("[") && content.endsWith("]")) {
                return content;
            }
        }

        // Tìm array pattern
        int startIndex = trimmed.indexOf('[');
        int endIndex = trimmed.lastIndexOf(']');
        if (startIndex != -1 && endIndex > startIndex) {
            return trimmed.substring(startIndex, endIndex + 1);
        }

        return trimmed;
    }

    /**
     * Tạo prompt cho quiz generation.
     */
    private String buildQuizPrompt(String summaryText, String numberOfQuestions, String difficultyLevel,
            String additionalRequirements) {

        // Thêm yêu cầu bổ sung nếu có
        String additionalSection = "";
        if (additionalRequirements != null && !additionalRequirements.trim().isEmpty()) {
            additionalSection = String.format("""

                    ---
                    YÊU CẦU BỔ SUNG TỪ NGƯỜI DÙNG:

                    %s

                    (Hãy ưu tiên tuân thủ yêu cầu bổ sung này khi tạo câu hỏi)
                    """, additionalRequirements.trim());
        }

        return String.format("""
                Bạn là chuyên gia thiết kế bài trắc nghiệm e-learning.

                Dưới đây là phần nội dung đã được tóm tắt từ nhiều tài liệu khác nhau
                trong notebook. Hãy dựa trên nội dung này để tạo câu hỏi trắc nghiệm:

                ---
                NỘI DUNG TÓM TẮT:

                %s

                ---%s

                Mục tiêu:
                - Tạo ra số lượng câu hỏi: %s (few = 3-5, standard = 6-10, many = 11-15)
                - Độ khó câu hỏi: %s (easy = 1, medium = 2, hard = 3)
                - Câu hỏi có thể là về khái niệm, quy trình, công thức, đoạn code, ví dụ ứng dụng, so sánh, phân tích...

                Format JSON response:
                [
                  {
                    "question": "Câu hỏi?",
                    "explanation": "Giải thích đáp án đúng",
                    "difficulty_level": 2,
                    "options": [
                      {"text": "Đáp án A", "is_correct": false, "feedback": "Phản hồi", "position": 1},
                      {"text": "Đáp án B", "is_correct": true, "feedback": "Phản hồi", "position": 2},
                      {"text": "Đáp án C", "is_correct": false, "feedback": "Phản hồi", "position": 3},
                      {"text": "Đáp án D", "is_correct": false, "feedback": "Phản hồi", "position": 4}
                    ]
                  }
                ]

                CHỈ TRẢ VỀ JSON ARRAY, KHÔNG CÓ TEXT KHÁC.
                """, summaryText, additionalSection, numberOfQuestions, difficultyLevel);
    }

    /**
     * Tạo prompt cho flashcard generation.
     */
    private String buildFlashcardPrompt(String summaryText, String numberOfCards, String additionalRequirements) {
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

    // ================================
    // DOCUMENT SUMMARIZATION
    // ================================

    /**
     * Tóm tắt nội dung từ nhiều files.
     */
    public String summarizeDocuments(List<NotebookFile> files, LlmModel llmModel) {
        if (files == null || files.isEmpty()) {
            return "";
        }

        int maxFiles = 10; // Cho phép tối đa 10 file
        int maxCharsTotal = 50000; // Tổng 50.000 ký tự (~8k–10k token)

        StringBuilder fullTextBuilder = new StringBuilder();
        int totalChars = 0;

        int limitFiles = Math.min(files.size(), maxFiles);

        for (int i = 0; i < limitFiles; i++) {
            NotebookFile file = files.get(i);
            String fileSummary = summarizeSingleFile(file, llmModel);

            if (fileSummary != null && !fileSummary.isEmpty()) {

                int remaining = maxCharsTotal - totalChars;
                if (remaining <= 0)
                    break;

                if (fileSummary.length() > remaining) {
                    fileSummary = fileSummary.substring(0, remaining);
                }

                fullTextBuilder.append("\n\n--- FILE: ")
                        .append(file.getOriginalFilename())
                        .append(" ---\n");

                fullTextBuilder.append(fileSummary);
                totalChars += fileSummary.length();
            }

            // Né rate limit Gemini khi xử lý file tiếp theo
            if (i < limitFiles - 1) {
                try {
                    System.out.println("⏳ [ASYNC] Chờ 10s trước file tiếp theo...");
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        return fullTextBuilder.toString().trim();
    }

    // ================================
    // AUDIO OVERVIEW (VOICE SCRIPT)
    // ================================

    /**
     * Tạo prompt và gọi Gemini để sinh JSON voice_script_overview cho Gemini TTS.
     *
     * @param files    danh sách files thuộc notebook
     * @param llmModel optional: chọn model, có thể null để dùng default
     * @return JSON string: {"voice_script_overview": "..."}
     */
    public String generateAudioOverviewJson(List<NotebookFile> files, LlmModel llmModel) {
        String summarized = summarizeDocuments(files, llmModel);
        if (summarized == null || summarized.isEmpty()) {
            throw new IllegalArgumentException("Không có nội dung để tạo audio overview.");
        }

        String prompt = """
                Bạn là biên tập viên nội dung Podcast giáo dục chuyên nghiệp.

                NHIỆM VỤ:
                Dựa trên nội dung tài liệu, hãy viết một kịch bản hội thoại ngắn (khoảng 150-200 từ) giữa hai nhân vật:
                1. **Host**: Người dẫn chương trình, đóng vai trò tò mò, đặt câu hỏi dẫn dắt hoặc tóm tắt ý.
                2. **Expert**: Chuyên gia, trả lời sâu sắc, giải thích nội dung từ tài liệu.

                DỮ LIỆU ĐẦU VÀO (tóm tắt):
                [SLIDE_JSON]

                YÊU CẦU ĐẦU RA:
                Trả về DUY NHẤT một JSON dạng:
                {
                  "voice_script_overview": "Host: Chào các bạn...\\nExpert: Xin chào..."
                }

                QUY TẮC QUAN TRỌNG:
                - Kịch bản PHẢI theo format chính xác:
                  Host: [Lời thoại]
                  Expert: [Lời thoại]
                - Không dùng markdown, không thêm text ngoài JSON.
                - Giọng văn tự nhiên, như văn nói, có cảm xúc.
                - Host nên hỏi những câu "Tại sao?", "Cụ thể là gì?" để Expert trả lời.
                """
                .replace("[SLIDE_JSON]", summarized);

        String response = aiModelService.callGeminiModel(prompt);
        if (response == null || response.trim().isEmpty()) {
            throw new RuntimeException("LLM không trả về nội dung audio overview.");
        }

        // Validate JSON và trường voice_script_overview để fail fast
        // Strip markdown wrapper nếu có (```json ... ```)
        String cleanedResponse = stripMarkdownWrapper(response);

        try {
            ObjectNode node = objectMapper.readValue(cleanedResponse, ObjectNode.class);
            if (!node.hasNonNull("voice_script_overview")) {
                throw new RuntimeException("JSON không có trường voice_script_overview.");
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("LLM trả về JSON không hợp lệ: " + e.getMessage(), e);
        }

        return cleanedResponse.trim();
    }

    /**
     * Strip markdown code block wrapper (```json ... ``` hoặc ``` ... ```).
     */
    private String stripMarkdownWrapper(String response) {
        if (response == null || response.isBlank()) {
            return response;
        }

        String trimmed = response.trim();

        // Nếu đã là JSON object hợp lệ
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed;
        }

        // Tìm trong code block ```json ... ``` hoặc ``` ... ```
        Pattern codeBlockPattern = Pattern.compile("```(?:json)?\\s*\\n?([\\s\\S]*?)```", Pattern.MULTILINE);
        Matcher matcher = codeBlockPattern.matcher(trimmed);
        if (matcher.find()) {
            String content = matcher.group(1).trim();
            if (content.startsWith("{") && content.endsWith("}")) {
                return content;
            }
        }

        // Tìm object pattern { ... }
        int startIndex = trimmed.indexOf('{');
        int endIndex = trimmed.lastIndexOf('}');
        if (startIndex != -1 && endIndex > startIndex) {
            return trimmed.substring(startIndex, endIndex + 1);
        }

        return trimmed;
    }

    // ================================
    // CALL GEMINI TTS + LƯU TtsAsset
    // ================================

    /**
     * Gọi Gemini TTS với voice script overview và lưu TtsAsset.
     * Sử dụng Gemini 2.5 Flash TTS API (REST) để tạo audio.
     *
     * @param script       nội dung voice_script_overview (plain text)
     * @param voiceId      tên giọng đọc (nếu null sẽ dùng "Kore" mặc định)
     * @param outputFormat không sử dụng (Gemini trả về PCM, convert sang WAV)
     * @param notebook     notebook sở hữu asset
     * @param user         người tạo
     * @param aiSet        liên kết NotebookAiSet (có thể null nếu chưa cần)
     * @return TtsAsset đã lưu
     */
    @Transactional
    public TtsAsset generateAudioOverviewAsset(
            String script,
            String voiceId, // Voice này sẽ dùng cho vai "Expert"
            String outputFormat,
            Notebook notebook,
            User user,
            NotebookAiSet aiSet) {

        if (script == null || script.trim().isEmpty()) {
            throw new IllegalArgumentException("Voice script overview trống.");
        }

        String geminiApiKey = geminiApiKeyConfig;
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            geminiApiKey = System.getenv("GOOGLE_API_KEY");
        }
        if (geminiApiKey == null || geminiApiKey.isEmpty()) {
            geminiApiKey = System.getenv("GEMINI_API_KEY");
        }
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            throw new IllegalStateException("Thiếu cấu hình API Key");
        }

        // Voice cho Expert (người dùng chọn hoặc mặc định Kore)
        String expertVoice = (voiceId != null && !voiceId.isBlank()) ? voiceId : "Kore";
        // Voice cho Host (Mặc định là Puck - giọng nam năng động)
        String hostVoice = "Puck";

        // Tối ưu text (vẫn giữ nguyên logic cũ)
        script = prepareTtsText(script);

        // QUAN TRỌNG: Thêm chỉ dẫn cho model biết đây là đoạn hội thoại
        // Model cần dòng này ở đầu để map đúng giọng vào đúng vai
        String conversationPrompt = "TTS the following conversation between Host and Expert:\n" + script;

        try {
            WebClient client = webClientBuilder
                    .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                    .build();

            // --- CẤU HÌNH MULTI-SPEAKER ---

            // 1. Cấu hình giọng Host
            Map<String, Object> hostConfig = Map.of(
                    "speaker", "Host",
                    "voiceConfig", Map.of("prebuiltVoiceConfig", Map.of("voiceName", hostVoice)));

            // 2. Cấu hình giọng Expert
            Map<String, Object> expertConfig = Map.of(
                    "speaker", "Expert",
                    "voiceConfig", Map.of("prebuiltVoiceConfig", Map.of("voiceName", expertVoice)));

            // 3. Gom vào MultiSpeakerVoiceConfig
            Map<String, Object> multiSpeakerConfig = Map.of(
                    "speakerVoiceConfigs", List.of(hostConfig, expertConfig));

            Map<String, Object> speechConfig = Map.of("multiSpeakerVoiceConfig", multiSpeakerConfig);

            // 4. Tạo Request Body
            Map<String, Object> generationConfig = Map.of(
                    "responseModalities", List.of("AUDIO"),
                    "speechConfig", speechConfig);

            // Lưu ý: Dùng conversationPrompt thay vì script gốc
            Map<String, Object> part = Map.of("text", conversationPrompt);
            Map<String, Object> content = Map.of("parts", List.of(part));

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(content),
                    "generationConfig", generationConfig
            // "model" để trên URL cũng được
            );

            String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-tts:generateContent";

            String responseJson = client.post()
                    .uri(apiUrl)
                    .header("x-goog-api-key", geminiApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (responseJson == null || responseJson.isEmpty()) {
                throw new RuntimeException("Gemini TTS trả về response rỗng.");
            }

            // Parse response (Logic giống cũ vì output structure không đổi)
            ObjectNode responseNode = objectMapper.readValue(responseJson, ObjectNode.class);
            JsonNode parts = responseNode.path("candidates").path(0).path("content").path("parts").path(0);

            // Check lỗi nếu model từ chối sinh audio
            if (!parts.has("inlineData")) {
                String textRes = parts.path("text").asText();
                throw new RuntimeException("Gemini từ chối sinh Audio. Lý do/Text: " + textRes);
            }

            String audioBase64 = parts.path("inlineData").path("data").asText();

            byte[] pcmBytes = java.util.Base64.getDecoder().decode(audioBase64);
            byte[] wavBytes = convertPcmToWav(pcmBytes, 24000, 1, 16);

            // Lưu file
            Path baseDir = Paths.get(uploadDir);
            Path ttsDir = baseDir.resolve("tts");
            Files.createDirectories(ttsDir);

            String filename = "audio_podcast_" + UUID.randomUUID() + ".wav";
            Path outPath = ttsDir.resolve(filename);
            Files.write(outPath, wavBytes);

            TtsAsset asset = TtsAsset.builder()
                    .notebook(notebook)
                    .createdBy(user)
                    .voiceName(hostVoice + " & " + expertVoice) // Lưu tên cả 2 giọng để dễ track
                    .textSource(script)
                    .audioUrl("/uploads/tts/" + filename)
                    .createdAt(OffsetDateTime.now())
                    .notebookAiSets(aiSet)
                    .build();

            return ttsAssetRepository.save(asset);

        } catch (Exception ex) {
            // Log full response nếu có lỗi để debug
            ex.printStackTrace();
            throw new RuntimeException("Lỗi gọi Gemini TTS Multi-Speaker: " + ex.getMessage(), ex);
        }
    }

    /**
     * Convert raw PCM audio bytes to WAV format.
     * PCM format: signed 16-bit little-endian
     */
    private byte[] convertPcmToWav(byte[] pcmData, int sampleRate, int numChannels, int bitsPerSample) {
        int byteRate = sampleRate * numChannels * bitsPerSample / 8;
        int blockAlign = numChannels * bitsPerSample / 8;
        int dataSize = pcmData.length;
        int chunkSize = 36 + dataSize;

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try {
            // RIFF header
            baos.write("RIFF".getBytes());
            baos.write(intToLittleEndian(chunkSize, 4));
            baos.write("WAVE".getBytes());

            // fmt subchunk
            baos.write("fmt ".getBytes());
            baos.write(intToLittleEndian(16, 4)); // Subchunk1Size (16 for PCM)
            baos.write(intToLittleEndian(1, 2)); // AudioFormat (1 = PCM)
            baos.write(intToLittleEndian(numChannels, 2));
            baos.write(intToLittleEndian(sampleRate, 4));
            baos.write(intToLittleEndian(byteRate, 4));
            baos.write(intToLittleEndian(blockAlign, 2));
            baos.write(intToLittleEndian(bitsPerSample, 2));

            // data subchunk
            baos.write("data".getBytes());
            baos.write(intToLittleEndian(dataSize, 4));
            baos.write(pcmData);

            return baos.toByteArray();
        } catch (java.io.IOException e) {
            throw new RuntimeException("Lỗi convert PCM to WAV: " + e.getMessage(), e);
        }
    }

    /**
     * Convert integer to little-endian byte array.
     */
    private byte[] intToLittleEndian(int value, int numBytes) {
        byte[] result = new byte[numBytes];
        for (int i = 0; i < numBytes; i++) {
            result[i] = (byte) ((value >> (8 * i)) & 0xFF);
        }
        return result;
    }

    /**
     * Tóm tắt nội dung của một file.
     */
    private String summarizeSingleFile(NotebookFile file, LlmModel llmModel) {
        int maxChunks = 8; // Đọc tối đa 8 chunk / file
        int maxCharsPerFile = 12000; // Cho phép mỗi file tối đa 12.000 ký tự
        int summaryThreshold = 4000; // Nếu dài hơn 4.000 ký tự → gọi LLM tóm tắt

        List<Object[]> chunkData = fileChunkRepository.findByFileIdWithLimit(file.getId(), maxChunks);
        if (chunkData == null || chunkData.isEmpty()) {
            return "";
        }

        StringBuilder textBuilder = new StringBuilder();
        int charCount = 0;

        for (Object[] row : chunkData) {
            if (charCount >= maxCharsPerFile)
                break;

            String content = (String) row[1];
            if (content != null && !content.isEmpty()) {
                int remaining = maxCharsPerFile - charCount;
                if (content.length() > remaining) {
                    content = content.substring(0, remaining);
                }
                textBuilder.append(content).append("\n");
                charCount += content.length();
            }
        }

        String fullText = textBuilder.toString().trim();
        if (fullText.isEmpty()) {
            return "";
        }

        // Nếu file quá dài → tóm tắt theo chunk
        if (fullText.length() > summaryThreshold) {
            return summarizeLongText(fullText, llmModel);
        }

        return fullText;
    }

    /**
     * Chia text dài thành chunks và tóm tắt từng phần bằng LLM.
     */
    private String summarizeLongText(String fullText, LlmModel llmModel) {
        int chunkSize = 3000; // mỗi chunk nhỏ đúng “nhẹ” cho free tier
        int overlap = 200;

        List<String> chunks = splitTextIntoChunks(fullText, chunkSize, overlap);
        StringBuilder summaryBuilder = new StringBuilder();

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);

            String chunkSummary = summarizeChunk(
                    chunk,
                    i,
                    chunks.size(),
                    fullText.length(),
                    llmModel);

            if (chunkSummary != null && !chunkSummary.isEmpty()) {
                summaryBuilder.append(chunkSummary).append("\n");
            }

            // Né rate-limit Google GEMINI FREE
            if (i < chunks.size() - 1) {
                try {
                    System.out.println("⏳ [ASYNC] Chờ 10s để né rate limit Gemini...");
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        return summaryBuilder.toString().trim();
    }

    /**
     * Chia text thành chunks với overlap.
     */
    private List<String> splitTextIntoChunks(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chunks;
        }

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            chunks.add(text.substring(start, end));

            // Move start for next chunk with overlap
            start = end - overlap;
            if (start >= text.length())
                break;
            if (end == text.length())
                break;
        }

        return chunks;
    }

    /**
     * Tóm tắt một chunk bằng LLM.
     */
    private String summarizeChunk(String chunk, int chunkIndex, int totalChunks, int originalLength,
            LlmModel llmModel) {
        try {
            String prompt = String.format("""
                     Tóm tắt đoạn văn bản sau (phần %d/%d của văn bản gốc %d ký tự):

                     ---
                     %s
                     ---

                    Yêu cầu:
                     - Chỉ giữ các ý quan trọng nhất.
                     - Viết súc tích, rõ ràng, không lan man.
                     - Không nhắc lại “phần x/y”, không thêm lời dẫn, không mở đầu hay kết thúc.
                     - Trả về đúng phần tóm tắt, không thêm bất kỳ câu nào ngoài nội dung.
                     """, chunkIndex + 1, totalChunks, originalLength, chunk);

            String response = aiModelService.callGeminiModel(prompt);
            return response != null ? response.trim() : "";
        } catch (Exception e) {
            System.err.println("❌ Lỗi tóm tắt chunk: " + e.getMessage());
            // Fallback: trả về chunk gốc đã cắt ngắn
            return chunk.length() > 500 ? chunk.substring(0, 500) + "..." : chunk;
        }
    }

    private String prepareTtsText(String script) {
        if (script == null)
            return "";

        String cleaned = script
                .replace("\n", " ")
                .replace("\t", " ")
                .replaceAll("\\s+", " ")
                .trim();

        // Tách câu rõ hơn để TTS đọc tự nhiên
        cleaned = cleaned.replaceAll("([a-zA-Z0-9]) ([A-Z])", "$1. $2");

        return cleaned;
    }

    // ================================
    // VIDEO GENERATION
    // ================================

    /**
     * Xử lý video generation ở background.
     * Pipeline: Summarize → LLM Plan → Render → TTS → Merge
     */
    @Async
    @Transactional
    public void processVideoGenerationAsync(UUID aiSetId, UUID notebookId, UUID userId,
            List<UUID> fileIds, String templateName, String additionalRequirements,
            int numberOfSlides, boolean generateImages) {

        String sessionId = aiSetId.toString().substring(0, 8);
        String videoTitle = "Video";

        try {
            System.out.println("🎬 [VIDEO] Session: " + sessionId + " | slides=" + numberOfSlides);
            updateAiSetStatus(aiSetId, "processing", null, null);

            // Validate entities
            Notebook notebook = notebookRepository.findById(notebookId).orElse(null);
            User user = userRepository.findById(userId).orElse(null);
            if (notebook == null || user == null) {
                updateAiSetStatus(aiSetId, "failed", "Notebook/User không tồn tại", null);
                return;
            }

            List<NotebookFile> files = fileIds.stream()
                    .map(id -> notebookFileRepository.findById(id).orElse(null))
                    .filter(f -> f != null)
                    .toList();
            if (files.isEmpty()) {
                updateAiSetStatus(aiSetId, "failed", "Không có file", null);
                return;
            }

            // Step 1: Summarize
            System.out.println("📝 [VIDEO] Step 1: Tóm tắt...");
            String summary = summarizeDocuments(files, null);
            if (summary == null || summary.isBlank()) {
                updateAiSetStatus(aiSetId, "failed", "Không thể tóm tắt", null);
                return;
            }

            // Step 2: LLM Plan
            System.out.println("🤖 [VIDEO] Step 2: Tạo plan...");
            String llmResponse = aiModelService
                    .callGeminiModel(buildVideoPrompt(summary, numberOfSlides, additionalRequirements));
            Map<String, Object> plan = parseVideoJson(llmResponse);
            if (plan == null) {
                updateAiSetStatus(aiSetId, "failed", "Không thể parse plan", null);
                return;
            }

            videoTitle = (String) plan.getOrDefault("title", "Video");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> slidesData = (List<Map<String, Object>>) plan.get("slides");
            if (slidesData == null || slidesData.isEmpty()) {
                updateAiSetStatus(aiSetId, "failed", "Không có slides", null);
                return;
            }

            // Build slides
            List<com.example.springboot_api.dto.shared.VideoSlide> slides = new ArrayList<>();
            for (int i = 0; i < slidesData.size(); i++) {
                Map<String, Object> sd = slidesData.get(i);
                slides.add(com.example.springboot_api.dto.shared.VideoSlide.builder()
                        .index(i)
                        .title((String) sd.get("title"))
                        .body((String) sd.get("body"))
                        .imagePrompt(generateImages ? (String) sd.get("imagePrompt") : null)
                        .audioScript((String) sd.get("audioScript"))
                        .build());
            }
            System.out.println("✅ [VIDEO] Plan: " + slides.size() + " slides, title: " + videoTitle);

            // Setup directories
            Path workDir = Paths.get("uploads", "videos", sessionId);
            Files.createDirectories(workDir.resolve("slides"));
            Files.createDirectories(workDir.resolve("audio"));
            Files.createDirectories(workDir.resolve("clips"));

            // Step 3: Render frames (trả về base64)
            System.out.println("🎨 [VIDEO] Step 3: Render frames...");
            List<String> frameBase64List = videoFrameService.renderVideoFrames(videoTitle,
                    slides.stream().map(s -> VideoFrameService.FrameContent.builder()
                            .title(s.getTitle()).body(s.getBody())
                            .imagePrompt(s.getImagePrompt()).audioScript(s.getAudioScript())
                            .build()).toList(),
                    generateImages);

            // Lưu base64 thành file PNG trong work directory
            for (int i = 0; i < Math.min(frameBase64List.size(), slides.size()); i++) {
                Path dst = workDir.resolve("slides").resolve(String.format("frame_%02d.png", i + 1));
                byte[] imageBytes = java.util.Base64.getDecoder().decode(frameBase64List.get(i));
                Files.write(dst, imageBytes);
                slides.get(i).setImagePath(dst.toString());
                slides.get(i).setImageReady(true);
            }

            // Step 4: Generate audio
            System.out.println("🔊 [VIDEO] Step 4: Generate audio...");
            for (var slide : slides) {
                try {
                    String script = slide.getAudioScript();
                    if (script == null || script.isBlank()) {
                        script = slide.getTitle() + ". "
                                + (slide.getBody() != null ? slide.getBody().replaceAll("[•\\-*]", "") : "");
                    }
                    Path audioPath = workDir.resolve("audio")
                            .resolve(String.format("slide_%02d.wav", slide.getIndex() + 1));
                    double duration = generateVideoTts(prepareTtsText(script), audioPath);
                    slide.setAudioPath(audioPath.toString());
                    slide.setAudioDuration(duration);
                    slide.setAudioReady(true);
                    System.out.println(
                            "  ✅ Audio " + (slide.getIndex() + 1) + ": " + String.format("%.1f", duration) + "s");
                    Thread.sleep(2500);
                } catch (Exception e) {
                    System.err.println("  ❌ Audio " + (slide.getIndex() + 1) + ": " + e.getMessage());
                }
            }

            // Step 5: Create clips
            System.out.println("🎬 [VIDEO] Step 5: Create clips...");
            List<Path> clipPaths = new ArrayList<>();
            for (var slide : slides) {
                if (slide.isImageReady() && slide.isAudioReady()) {
                    Path clipPath = workDir.resolve("clips")
                            .resolve(String.format("clip_%02d.mp4", slide.getIndex() + 1));
                    if (createClip(slide.getImagePath(), slide.getAudioPath(), slide.getAudioDuration(), clipPath)) {
                        clipPaths.add(clipPath);
                    }
                }
            }

            // Step 6: Merge
            Path finalVideo = workDir.resolve("final.mp4");
            if (!clipPaths.isEmpty()) {
                System.out.println("🎬 [VIDEO] Step 6: Merge " + clipPaths.size() + " clips...");
                mergeClips(clipPaths, workDir, finalVideo);
            }

            // Finalize
            if (Files.exists(finalVideo)) {
                String fileName = "video_" + sessionId + ".mp4";
                Path destPath = Paths.get("uploads", "videos", fileName);
                Files.move(finalVideo, destPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                cleanupDirectory(workDir);

                double totalDuration = slides.stream().mapToDouble(s -> s.getAudioDuration()).sum();
                String videoUrl = "/uploads/videos/" + fileName;

                // Save VideoAsset
                NotebookAiSet aiSet = aiSetRepository.findById(aiSetId).orElse(null);
                var videoAsset = com.example.springboot_api.models.VideoAsset.builder()
                        .notebook(notebook).createdBy(user).style(templateName)
                        .textSource(videoTitle).videoUrl(videoUrl)
                        .durationSeconds((int) totalDuration).createdAt(OffsetDateTime.now())
                        .notebookAiSets(aiSet).build();
                videoAssetRepository.save(videoAsset);

                // Update AiSet title
                if (aiSet != null) {
                    aiSet.setTitle(videoTitle);
                    aiSetRepository.save(aiSet);
                }

                Map<String, Object> stats = Map.of(
                        "slideCount", slides.size(), "clipCount", clipPaths.size(),
                        "title", videoTitle, "videoUrl", videoUrl,
                        "videoAssetId", videoAsset.getId().toString(),
                        "totalDuration", totalDuration);
                updateAiSetStatus(aiSetId, "done", null, stats);
                System.out.println("🎉 [VIDEO] Done! " + destPath);
            } else {
                updateAiSetStatus(aiSetId, "failed", "Video merge failed", Map.of("title", videoTitle));
            }

        } catch (Exception e) {
            updateAiSetStatus(aiSetId, "failed", "Error: " + e.getMessage(), null);
            System.err.println("❌ [VIDEO] " + e.getMessage());
        }
    }

    private void cleanupDirectory(Path dir) {
        try {
            if (Files.exists(dir)) {
                Files.walk(dir).sorted(java.util.Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (Exception ignored) {
                            }
                        });
            }
        } catch (Exception e) {
            System.err.println("⚠️ Cleanup failed: " + e.getMessage());
        }
    }

    private String buildVideoPrompt(String summary, int slides, String extra) {
        String additional = (extra != null && !extra.isBlank()) ? "\nYêu cầu thêm: " + extra : "";
        return String.format(
                """
                        Bạn là YouTuber giáo dục nổi tiếng, tạo video giải thích dễ hiểu và cuốn hút.

                        TẠO SCRIPT VIDEO GỒM %d SLIDES từ nội dung sau:
                        ---
                        %s
                        ---%s

                        THÔNG TIN KÊNH:
                        - Video do nhóm F4 phát triển
                        - Kênh NotebookAI - Công cụ học tập thông minh

                        QUY TẮC QUAN TRỌNG:
                        1. VIDEO PHẢI CÓ FLOW LIÊN TỤC - mỗi slide nối tiếp slide trước như một câu chuyện
                        2. Slide ĐẦU TIÊN (INTRO): Chào đón, giới thiệu nhóm F4 phát triển video, nói rõ video này sẽ tìm hiểu về gì
                        3. Slide CUỐI CÙNG (OUTRO): Tóm tắt nội dung đã học, cảm ơn, kêu gọi like/subscribe kênh NotebookAI
                        4. Các slide giữa giải thích từng ý một cách TUẦN TỰ, có câu chuyển tiếp mượt mà

                        CHO MỖI SLIDE:
                        - title: Tiêu đề ngắn gọn (tối đa 10 từ)
                        - body: 2-3 bullet points ngắn (hiển thị trên màn hình)
                        - imagePrompt: Mô tả hình ảnh minh họa (tiếng Anh, cartoon/illustration style, colorful, friendly)
                        - audioScript: SCRIPT ĐẦY ĐỦ để đọc (80-120 từ), viết như đang nói chuyện tự nhiên, xưng "mình" với "các bạn"

                        VÍ DỤ audioScript:
                        - INTRO: "Chào các bạn! Video này do nhóm F4 gồm Huỳnh, Tuấn, An, Truyền phát triển để mang đến cho các bạn cách nhìn hay nhất về [chủ đề]. Hôm nay mình sẽ cùng các bạn tìm hiểu về [nội dung cụ thể]. Đây là kiến thức rất thú vị và mình tin các bạn sẽ thấy hữu ích. Bây giờ mình cùng bắt đầu nhé!"
                        - Content: "Được rồi, tiếp theo mình sẽ giải thích về [ý chính]. [Giải thích chi tiết 2-3 câu]. Ví dụ như [ví dụ thực tế]. Các bạn thấy không, khi hiểu được điều này thì mọi thứ sẽ dễ dàng hơn rất nhiều."
                        - OUTRO: "Vậy là mình đã cùng các bạn tìm hiểu xong về [chủ đề]. Tóm lại, [điểm chính 1], [điểm chính 2]. Hy vọng video này hữu ích cho các bạn. Nếu thấy hay, đừng quên bấm like và đăng ký kênh NotebookAI của nhóm F4 nhé. Hẹn gặp lại các bạn trong video tiếp theo!"

                        LƯU Ý QUAN TRỌNG:
                        - audioScript phải HOÀN CHỈNH, đọc được trọn vẹn, không cắt giữa chừng
                        - Có câu nối mượt giữa các slide: "Được rồi, tiếp theo...", "Bây giờ mình sẽ...", "Một điều quan trọng nữa là..."
                        - Giọng văn thân thiện, gần gũi như đang trò chuyện với bạn bè
                        - Không dùng ký tự đặc biệt như *, #, markdown

                        TRẢ VỀ JSON (KHÔNG có markdown):
                        {"title": "Tên video hấp dẫn", "slides": [{"title": "...", "body": "• Point 1\\n• Point 2", "imagePrompt": "...", "audioScript": "..."}]}
                        """,
                slides, summary, additional);
    }

    private Map<String, Object> parseVideoJson(String response) {
        try {
            String json = extractJsonFromResponse(response);
            if (json == null)
                return null;
            System.out.println("📝 [VIDEO] JSON: " + json.substring(0, Math.min(150, json.length())) + "...");

            json = json.trim();
            if (json.startsWith("[")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> slides = objectMapper.readValue(json, List.class);
                return Map.of("title", "Video", "slides", slides);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(json, Map.class);
            return data;
        } catch (Exception e) {
            System.err.println("❌ Parse JSON: " + e.getMessage());
            return null;
        }
    }

    private double generateVideoTts(String text, Path outputPath) throws Exception {
        String apiKey = geminiApiKeyConfig != null && !geminiApiKeyConfig.isBlank()
                ? geminiApiKeyConfig
                : System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank())
            throw new IllegalStateException("Missing API Key");

        WebClient client = webClientBuilder.codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)).build();
        String resp = client.post()
                .uri("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-tts:generateContent")
                .header("x-goog-api-key", apiKey).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", text)))),
                        "generationConfig", Map.of("responseModalities", List.of("AUDIO"),
                                "speechConfig",
                                Map.of("voiceConfig", Map.of("prebuiltVoiceConfig", Map.of("voiceName", "Aoede"))))))
                .retrieve().bodyToMono(String.class).block();

        JsonNode data = objectMapper.readTree(resp).path("candidates").path(0).path("content").path("parts").path(0)
                .path("inlineData");
        if (!data.has("data"))
            throw new RuntimeException("No audio");

        byte[] pcm = java.util.Base64.getDecoder().decode(data.path("data").asText());
        Files.write(outputPath, convertPcmToWav(pcm, 24000, 1, 16));
        return (double) pcm.length / (24000.0 * 2);
    }

    private boolean createClip(String img, String audio, double duration, Path out) {
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-y", "-loop", "1", "-i", img, "-i", audio,
                    "-c:v", "libx264", "-tune", "stillimage", "-c:a", "aac", "-b:a", "192k",
                    "-pix_fmt", "yuv420p", "-t", String.format("%.2f", duration), out.toString());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.getInputStream().transferTo(java.io.OutputStream.nullOutputStream());
            return p.waitFor() == 0 && Files.exists(out);
        } catch (Exception e) {
            return false;
        }
    }

    private void mergeClips(List<Path> clips, Path dir, Path out) {
        try {
            Path list = dir.resolve("clips.txt");
            Files.write(list, clips.stream().map(p -> "file '" + p.toAbsolutePath() + "'").toList());
            new ProcessBuilder("ffmpeg", "-y", "-f", "concat", "-safe", "0", "-i", list.toString(), "-c", "copy",
                    out.toString())
                    .redirectErrorStream(true).start().waitFor();
        } catch (Exception e) {
            System.err.println("Merge error: " + e.getMessage());
        }
    }

    // ================================
    // MINDMAP / SUGGESTION (TODO)
    // ================================
    @Async
    @Transactional
    public void processMindmapGenerationAsync(UUID aiSetId, UUID notebookId, UUID userId, List<UUID> fileIds,
            String additionalRequirements) {
        updateAiSetStatus(aiSetId, "failed", "Mindmap chưa implement", null);
    }

    @Async
    @Transactional
    public void processSuggestionGenerationAsync(UUID aiSetId, UUID notebookId, UUID userId, List<UUID> fileIds,
            String additionalRequirements) {
        updateAiSetStatus(aiSetId, "failed", "Suggestion chưa implement", null);
    }
}
