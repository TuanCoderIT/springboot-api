package com.example.springboot_api.services.shared.ai;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot_api.common.exceptions.NotFoundException;
import com.example.springboot_api.dto.user.quiz.attempt.QuizAnalysisResponse;
import com.example.springboot_api.models.QuizAttempt;
import com.example.springboot_api.models.QuizAttemptAnswer;
import com.example.springboot_api.repositories.shared.QuizAttemptAnswerRepository;
import com.example.springboot_api.repositories.shared.QuizAttemptRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service gọi AI phân tích kết quả quiz - có so sánh xuyên notebook.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuizAnalysisService {

    private final QuizAttemptRepository attemptRepository;
    private final QuizAttemptAnswerRepository answerRepository;
    private final AIModelService aiModelService;
    private final JsonParsingService jsonParsingService;

    private static final int MAX_HISTORY_ATTEMPTS = 10;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Phân tích kết quả quiz bằng AI - có so sánh với lịch sử XUYÊN NOTEBOOK.
     */
    @Transactional
    public QuizAnalysisResponse analyzeAttempt(UUID attemptId) {
        log.info("🧠 [QUIZ_ANALYSIS] Analyzing attempt with notebook-wide history: {}", attemptId);

        QuizAttempt currentAttempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy attempt"));

        // Load current answers
        List<QuizAttemptAnswer> currentAnswers = answerRepository.findByAttemptId(attemptId);
        if (currentAnswers.isEmpty()) {
            throw new NotFoundException("Không có câu trả lời nào để phân tích");
        }

        // Lấy notebookId từ aiSet
        UUID userId = currentAttempt.getUser().getId();
        UUID notebookId = currentAttempt.getNotebookAiSet().getNotebook().getId();

        // Lấy 10 attempts gần nhất trong NOTEBOOK (cross-quiz)
        List<QuizAttempt> recentAttempts = attemptRepository.findRecentByNotebook(
                userId, notebookId, PageRequest.of(0, MAX_HISTORY_ATTEMPTS));

        // Lọc bỏ current attempt
        List<QuizAttempt> historyAttempts = recentAttempts.stream()
                .filter(a -> !a.getId().equals(attemptId))
                .toList();

        // Load tất cả answers từ history
        List<HistoryQuizData> historyData = new ArrayList<>();
        for (QuizAttempt attempt : historyAttempts) {
            List<QuizAttemptAnswer> answers = answerRepository.findByAttemptId(attempt.getId());
            historyData.add(new HistoryQuizData(attempt, answers));
        }

        // Build prompt với lịch sử xuyên notebook
        String prompt = buildCrossNotebookPrompt(currentAttempt, currentAnswers, historyData);

        // Call AI
        log.info("🤖 [QUIZ_ANALYSIS] Calling AI with notebook-wide history ({} previous attempts)...",
                historyData.size());
        String llmResponse = aiModelService.callGeminiModel(prompt);

        // Parse response
        Map<String, Object> analysisData = jsonParsingService.parseJsonObject(llmResponse);
        if (analysisData == null) {
            log.error("❌ [QUIZ_ANALYSIS] Failed to parse AI response");
            return buildFallbackAnalysis(currentAttempt);
        }

        // Save analysis to attempt
        currentAttempt.setAnalysisJson(analysisData);
        attemptRepository.save(currentAttempt);

        log.info("✅ [QUIZ_ANALYSIS] Analysis completed for attempt: {}", attemptId);

        return toAnalysisResponse(analysisData, currentAttempt);
    }

    /**
     * Lấy analysis đã lưu (nếu có).
     */
    @Transactional(readOnly = true)
    public QuizAnalysisResponse getSavedAnalysis(UUID attemptId) {
        QuizAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy attempt"));

        if (attempt.getAnalysisJson() == null) {
            return null;
        }

        return toAnalysisResponse(attempt.getAnalysisJson(), attempt);
    }

    /**
     * Build prompt với lịch sử xuyên notebook.
     * AI sẽ tự so sánh các CHỦ ĐỀ tương tự qua các bộ quiz khác nhau.
     */
    private String buildCrossNotebookPrompt(QuizAttempt currentAttempt,
            List<QuizAttemptAnswer> currentAnswers,
            List<HistoryQuizData> historyData) {

        StringBuilder sb = new StringBuilder();
        sb.append(
                "Bạn là AI phân tích kết quả học tập. Hãy phân tích kết quả làm quiz VÀ SO SÁNH TIẾN BỘ với các lần làm trước.\n\n");

        // === KẾT QUẢ LẦN NÀY ===
        sb.append("═══════════════════════════════════════\n");
        sb.append("KẾT QUẢ LẦN NÀY\n");
        sb.append("═══════════════════════════════════════\n");
        String currentTime = currentAttempt.getCreatedAt() != null
                ? currentAttempt.getCreatedAt().format(DATE_FMT)
                : "N/A";
        sb.append("• Thời điểm: ").append(currentTime).append("\n");
        sb.append("• Bộ quiz: ").append(currentAttempt.getNotebookAiSet().getTitle()).append("\n");
        sb.append("• Điểm: ").append(currentAttempt.getCorrectCount())
                .append("/").append(currentAttempt.getTotalQuestions()).append("\n\n");

        sb.append("CHI TIẾT:\n");
        int index = 1;
        for (QuizAttemptAnswer ans : currentAnswers) {
            if (ans.getQuiz() == null)
                continue;
            String question = ans.getQuiz().getQuestion();
            boolean correct = Boolean.TRUE.equals(ans.getIsCorrect());
            sb.append(index++).append(". ").append(question).append("\n");
            sb.append("   → Kết quả: ").append(correct ? "ĐÚNG ✓" : "SAI ✗").append("\n");
        }

        // === LỊCH SỬ CÁC LẦN TRƯỚC ===
        if (!historyData.isEmpty()) {
            sb.append("\n═══════════════════════════════════════\n");
            sb.append("LỊCH SỬ CÁC LẦN LÀM TRƯỚC (gần đây nhất ← cũ hơn)\n");
            sb.append("═══════════════════════════════════════\n");

            for (int i = 0; i < historyData.size(); i++) {
                HistoryQuizData hd = historyData.get(i);
                String time = hd.attempt.getCreatedAt() != null
                        ? hd.attempt.getCreatedAt().format(DATE_FMT)
                        : "N/A";
                String title = hd.attempt.getNotebookAiSet().getTitle();

                sb.append("\n📅 Lần ").append(i + 1).append(": ").append(time).append("\n");
                sb.append("   Bộ quiz: ").append(title).append("\n");
                sb.append("   Điểm: ").append(hd.attempt.getCorrectCount())
                        .append("/").append(hd.attempt.getTotalQuestions()).append("\n");
                sb.append("   Câu sai:\n");

                for (QuizAttemptAnswer ans : hd.answers) {
                    if (ans.getQuiz() == null)
                        continue;
                    if (!Boolean.TRUE.equals(ans.getIsCorrect())) {
                        sb.append("      - ").append(ans.getQuiz().getQuestion()).append("\n");
                    }
                }
            }
        }

        // === YÊU CẦU ===
        sb.append(
                """

                        ═══════════════════════════════════════
                        YÊU CẦU PHÂN TÍCH
                        ═══════════════════════════════════════
                        Dựa trên kết quả lần này và LỊCH SỬ các lần trước, hãy phân tích:
                        1. Các CHỦ ĐỀ mà người dùng còn yếu (lặp lại sai nhiều lần)
                        2. Các CHỦ ĐỀ đã CẢI THIỆN (trước sai, nay đúng - dù câu hỏi khác nhưng cùng chủ đề)
                        3. Kiến thức MỚI đã nắm được

                        Trả về JSON theo format:
                        {
                          "scoreText": "7/10 (70%)",
                          "summary": "Tóm tắt ngắn gọn tiến bộ của người dùng",
                          "strengths": [
                            {"topic": "Chủ đề mạnh", "analysis": "Phân tích...", "suggestions": []}
                          ],
                          "weaknesses": [
                            {"topic": "Chủ đề yếu (lặp lại sai)", "analysis": "Phân tích, nói rõ đã sai bao nhiêu lần", "suggestions": ["Gợi ý học"]}
                          ],
                          "improvements": [
                            {"topic": "Chủ đề đã cải thiện", "analysis": "Trước đây sai về X, nay đã đúng"}
                          ],
                          "recommendations": ["Gợi ý tổng thể"]
                        }

                        QUAN TRỌNG:
                        - So sánh theo CHỦ ĐỀ, không phải theo câu hỏi cụ thể
                        - Nếu chủ đề giống nhau ở các bộ quiz khác nhau thì vẫn so sánh được
                        - Nhấn mạnh các chủ đề LẶP LẠI SAI nhiều lần

                        CHỈ TRẢ VỀ JSON, KHÔNG CÓ TEXT KHÁC.
                        """);

        return sb.toString();
    }

    private QuizAnalysisResponse buildFallbackAnalysis(QuizAttempt attempt) {
        int score = attempt.getScore() != null ? attempt.getScore() : 0;
        String scoreText = attempt.getCorrectCount() + "/" + attempt.getTotalQuestions() + " (" + score + "%)";

        return QuizAnalysisResponse.builder()
                .scoreText(scoreText)
                .summary("Không thể phân tích chi tiết. Vui lòng thử lại.")
                .strengths(List.of())
                .weaknesses(List.of())
                .improvements(List.of())
                .recommendations(List.of("Hãy xem lại các câu trả lời sai và học lại phần kiến thức liên quan."))
                .build();
    }

    @SuppressWarnings("unchecked")
    private QuizAnalysisResponse toAnalysisResponse(Map<String, Object> data, QuizAttempt attempt) {
        String scoreText = (String) data.getOrDefault("scoreText",
                attempt.getCorrectCount() + "/" + attempt.getTotalQuestions());
        String summary = (String) data.getOrDefault("summary", "");

        List<QuizAnalysisResponse.TopicAnalysis> strengths = parseTopicList(data.get("strengths"));
        List<QuizAnalysisResponse.TopicAnalysis> weaknesses = parseTopicList(data.get("weaknesses"));
        List<QuizAnalysisResponse.TopicAnalysis> improvements = parseTopicList(data.get("improvements"));

        List<String> recommendations = new ArrayList<>();
        if (data.get("recommendations") instanceof List) {
            recommendations = (List<String>) data.get("recommendations");
        }

        return QuizAnalysisResponse.builder()
                .scoreText(scoreText)
                .summary(summary)
                .strengths(strengths)
                .weaknesses(weaknesses)
                .improvements(improvements)
                .recommendations(recommendations)
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<QuizAnalysisResponse.TopicAnalysis> parseTopicList(Object obj) {
        List<QuizAnalysisResponse.TopicAnalysis> result = new ArrayList<>();
        if (obj instanceof List) {
            for (Object item : (List<Object>) obj) {
                if (item instanceof Map) {
                    Map<String, Object> m = (Map<String, Object>) item;
                    result.add(QuizAnalysisResponse.TopicAnalysis.builder()
                            .topic((String) m.get("topic"))
                            .analysis((String) m.get("analysis"))
                            .suggestions(m.get("suggestions") instanceof List
                                    ? (List<String>) m.get("suggestions")
                                    : List.of())
                            .build());
                }
            }
        }
        return result;
    }

    /**
     * Helper record cho lịch sử quiz.
     */
    private record HistoryQuizData(QuizAttempt attempt, List<QuizAttemptAnswer> answers) {
    }
}
