package com.example.springboot_api.services.shared.ai;

import java.time.OffsetDateTime;
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
import com.fasterxml.jackson.databind.ObjectMapper;

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
    private final ObjectMapper objectMapper;

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

        // Nếu đã là JSON array hợp lệ
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

        // Giới hạn số lượng files để tránh OutOfMemoryError
        int maxFiles = 10;
        int maxCharsTotal = 9000;

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

                fullTextBuilder.append("\n\n--- FILE: ").append(file.getOriginalFilename()).append(" ---\n");
                fullTextBuilder.append(fileSummary);
                totalChars += fileSummary.length() + 50; // buffer cho header
            }
        }

        return fullTextBuilder.toString().trim();
    }

    /**
     * Tóm tắt nội dung của một file.
     */
    private String summarizeSingleFile(NotebookFile file, LlmModel llmModel) {
        int maxChunks = 5;
        int maxCharsPerFile = 3000;
        int summaryThreshold = 2500;

        // Lấy một số chunks theo thứ tự index (giới hạn để tránh OutOfMemoryError)
        List<Object[]> chunkData = fileChunkRepository.findByFileIdWithLimit(file.getId(), maxChunks);
        if (chunkData == null || chunkData.isEmpty()) {
            return "";
        }

        StringBuilder textBuilder = new StringBuilder();
        int charCount = 0;

        for (Object[] row : chunkData) {
            if (charCount >= maxCharsPerFile)
                break;

            String content = (String) row[1]; // index 1 is content
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

        // Nếu text quá dài, tóm tắt bằng LLM
        if (fullText.length() > summaryThreshold) {
            return summarizeLongText(fullText, llmModel);
        }

        return fullText;
    }

    /**
     * Chia text dài thành chunks và tóm tắt từng phần bằng LLM.
     */
    private String summarizeLongText(String fullText, LlmModel llmModel) {
        int chunkSize = 2000;
        int overlap = 200;

        List<String> chunks = splitTextIntoChunks(fullText, chunkSize, overlap);
        StringBuilder summaryBuilder = new StringBuilder();

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            String chunkSummary = summarizeChunk(chunk, i, chunks.size(), fullText.length(), llmModel);
            if (chunkSummary != null && !chunkSummary.isEmpty()) {
                summaryBuilder.append(chunkSummary).append("\n");
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
                    - Giữ lại các thông tin quan trọng, khái niệm chính
                    - Tóm tắt ngắn gọn, súc tích
                    - Chỉ trả về nội dung tóm tắt, không có text thêm
                    """, chunkIndex + 1, totalChunks, originalLength, chunk);

            String response = aiModelService.callGeminiModel(prompt);
            return response != null ? response.trim() : "";
        } catch (Exception e) {
            System.err.println("❌ Lỗi tóm tắt chunk: " + e.getMessage());
            // Fallback: trả về chunk gốc đã cắt ngắn
            return chunk.length() > 500 ? chunk.substring(0, 500) + "..." : chunk;
        }
    }
}
