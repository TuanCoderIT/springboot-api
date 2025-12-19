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
import com.example.springboot_api.models.NotebookFile;
import com.example.springboot_api.models.NotebookQuizOption;
import com.example.springboot_api.models.NotebookQuizz;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.shared.NotebookAiSetRepository;
import com.example.springboot_api.repositories.shared.QuizOptionRepository;
import com.example.springboot_api.repositories.shared.QuizRepository;
import com.example.springboot_api.services.shared.ai.AIModelService;
import com.example.springboot_api.services.shared.ai.AiSetStatusService;
import com.example.springboot_api.services.shared.ai.DocumentSummarizationService;
import com.example.springboot_api.services.shared.ai.JsonParsingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service xử lý quiz generation.
 * Bao gồm: tạo prompt, gọi LLM, parse response, lưu database.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuizGenerationService {

    private final NotebookAiSetRepository aiSetRepository;
    private final QuizRepository quizRepository;
    private final QuizOptionRepository quizOptionRepository;
    private final AIModelService aiModelService;
    private final DocumentSummarizationService summarizationService;
    private final JsonParsingService jsonParsingService;
    private final AiSetStatusService statusService;

    /**
     * Xử lý quiz generation ở background (async).
     */
    @Async
    @Transactional
    public void processQuizGenerationAsync(UUID aiSetId, UUID notebookId, UUID userId,
            List<UUID> fileIds, String numberOfQuestions, String difficultyLevel,
            String additionalRequirements) {

        log.info("🚀 [QUIZ] Bắt đầu tạo quiz - AiSet: {} | Thread: {}", aiSetId, Thread.currentThread().getName());

        try {
            statusService.markProcessing(aiSetId);

            NotebookAiSet aiSet = aiSetRepository.findById(aiSetId).orElse(null);
            if (aiSet == null) {
                log.error("❌ [QUIZ] Không tìm thấy AiSet: {}", aiSetId);
                return;
            }

            Notebook notebook = aiSet.getNotebook();
            User user = aiSet.getCreatedBy();

            if (notebook == null || user == null) {
                statusService.markFailed(aiSetId, "Không tìm thấy notebook hoặc user từ AiSet");
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
                statusService.markFailed(aiSetId, "Không tìm thấy file nào từ AiSet");
                return;
            }

            // Tóm tắt documents
            log.info("📄 [QUIZ] Đang tóm tắt tài liệu...");
            String summaryText = summarizationService.summarizeDocuments(selectedFiles, null);
            if (summaryText == null || summaryText.isEmpty()) {
                statusService.markFailed(aiSetId, "Không thể tóm tắt tài liệu (có thể không có chunks)");
                return;
            }

            // Tạo prompt cho quiz
            String quizPrompt = buildQuizPrompt(summaryText, numberOfQuestions, difficultyLevel,
                    additionalRequirements);

            // Gọi LLM để tạo quiz
            log.info("🤖 [QUIZ] Đang gọi LLM...");
            String llmResponse = aiModelService.callGeminiModel(quizPrompt);
            if (llmResponse == null || llmResponse.trim().isEmpty()) {
                statusService.markFailed(aiSetId, "LLM trả về response rỗng");
                return;
            }

            // Parse JSON và lưu quiz vào database
            log.info("💾 [QUIZ] Đang lưu quiz vào database...");
            List<Map<String, Object>> quizList = jsonParsingService.parseJsonArray(llmResponse);
            if (quizList == null || quizList.isEmpty()) {
                statusService.markFailed(aiSetId, "Không thể parse quiz từ LLM response");
                return;
            }

            // Lưu quiz vào database
            List<UUID> savedQuizIds = saveQuizzesToDatabase(notebook, user, aiSet, quizList);

            // Cập nhật AiSet thành công
            Map<String, Object> outputStats = new HashMap<>();
            outputStats.put("quizIds", savedQuizIds);
            outputStats.put("quizCount", savedQuizIds.size());
            statusService.markDone(aiSetId, outputStats);

            log.info("✅ [QUIZ] Hoàn thành tạo quiz - AiSet: {} | Số quiz: {}", aiSetId, savedQuizIds.size());

        } catch (Exception e) {
            String errorMsg = "Lỗi khi tạo quiz: " + e.getMessage();
            statusService.markFailed(aiSetId, errorMsg);
            log.error("❌ [QUIZ] {}", errorMsg, e);
        }
    }

    /**
     * Tạo prompt cho quiz generation.
     */
    public String buildQuizPrompt(String summaryText, String numberOfQuestions, String difficultyLevel,
            String additionalRequirements) {

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
     * Lưu quiz vào database với foreign key tới NotebookAiSet.
     */
    @Transactional
    public List<UUID> saveQuizzesToDatabase(Notebook notebook, User user, NotebookAiSet aiSet,
            List<Map<String, Object>> quizList) {

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
                    .notebookAiSets(aiSet)
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
}
