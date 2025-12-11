package com.example.springboot_api.services.shared.ai;

import java.time.OffsetDateTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.springboot_api.models.LlmModel;
import com.example.springboot_api.models.Notebook;
import com.example.springboot_api.models.NotebookAiSet;
import com.example.springboot_api.models.NotebookFile;
import com.example.springboot_api.models.NotebookQuizOption;
import com.example.springboot_api.models.NotebookQuizz;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.shared.FileChunkRepository;
import com.example.springboot_api.repositories.shared.NotebookAiSetRepository;
import com.example.springboot_api.repositories.shared.QuizOptionRepository;
import com.example.springboot_api.repositories.shared.QuizRepository;
import com.example.springboot_api.repositories.shared.TtsAssetRepository;
import com.example.springboot_api.models.TtsAsset;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.core.JsonProcessingException;

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
    private final AIModelService aiModelService;
    private final TtsAssetRepository ttsAssetRepository;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;

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

}
