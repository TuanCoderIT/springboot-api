package com.example.springboot_api.services.exam;

import com.example.springboot_api.dto.exam.GenerateQuestionsRequest;
import com.example.springboot_api.models.NotebookFile;
import com.example.springboot_api.models.exam.*;
import com.example.springboot_api.services.shared.ai.AIModelService;
import com.example.springboot_api.services.shared.ai.DocumentSummarizationService;
import com.example.springboot_api.services.shared.ai.JsonParsingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for generating exam questions from notebook files using AI
 * Reuses the existing quiz generation logic from QuizGenerationService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionGenerationService {
    
    private final AIModelService aiModelService;
    private final DocumentSummarizationService documentSummarizationService;
    private final JsonParsingService jsonParsingService;
    private final ObjectMapper objectMapper;
    
    /**
     * Generate questions from notebook files using AI
     */
    public List<ExamQuestion> generateQuestions(Exam exam, List<NotebookFile> files, 
                                              GenerateQuestionsRequest request) {
        log.info("Generating {} questions for exam {} from {} files using AI", 
                request.getNumberOfQuestions(), exam.getId(), files.size());
        
        try {
            // 1. Summarize documents from files
            log.info("📄 [EXAM] Summarizing documents...");
            String summaryText = documentSummarizationService.summarizeDocuments(files, null);
            if (summaryText == null || summaryText.isEmpty()) {
                throw new RuntimeException("Cannot summarize documents (no content found)");
            }
            
            // 2. Build prompt for question generation
            String prompt = buildExamQuestionPrompt(summaryText, request);
            
            // 3. Call AI model to generate questions
            log.info("🤖 [EXAM] Calling AI model...");
            String llmResponse = aiModelService.callGeminiModel(prompt);
            if (llmResponse == null || llmResponse.trim().isEmpty()) {
                throw new RuntimeException("AI model returned empty response");
            }
            
            // 4. Parse JSON response
            log.info("📝 [EXAM] Parsing AI response...");
            List<Map<String, Object>> questionList = jsonParsingService.parseJsonArray(llmResponse);
            if (questionList == null || questionList.isEmpty()) {
                throw new RuntimeException("Cannot parse questions from AI response");
            }
            
            // 5. Convert to ExamQuestion entities
            List<ExamQuestion> questions = convertToExamQuestions(exam, questionList, request);
            
            log.info("✅ [EXAM] Successfully generated {} questions for exam {}", questions.size(), exam.getId());
            return questions;
            
        } catch (Exception e) {
            log.error("❌ [EXAM] Error generating questions for exam {}: {}", exam.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate questions: " + e.getMessage(), e);
        }
    }
    
    /**
     * Build prompt for exam question generation (similar to quiz prompt but for exams)
     */
    private String buildExamQuestionPrompt(String summaryText, GenerateQuestionsRequest request) {
        
        String additionalSection = "";
        if (request.getIncludeExplanation()) {
            additionalSection += "\n- Bao gồm giải thích chi tiết cho mỗi câu hỏi";
        }
        
        String difficultyInstruction = getDifficultyInstruction(request.getDifficultyLevel());
        String questionTypeInstruction = getQuestionTypeInstruction(request.getQuestionTypes());
        
        return String.format("""
                Bạn là chuyên gia thiết kế đề thi trực tuyến cho hệ thống giáo dục.

                Dưới đây là nội dung đã được tóm tắt từ tài liệu học tập:

                ---
                NỘI DUNG TÓM TẮT:

                %s

                ---

                YÊU CẦU TẠO ĐỀ THI:
                - Số lượng câu hỏi: %d
                - Loại câu hỏi: %s
                - Độ khó: %s
                - Ngôn ngữ: %s
                - Số lựa chọn cho MCQ: %d%s

                %s

                %s

                Format JSON response (CHỈ TRẢ VỀ JSON ARRAY, KHÔNG CÓ TEXT KHÁC):
                [
                  {
                    "question": "Câu hỏi chi tiết và rõ ràng?",
                    "type": "MCQ",
                    "explanation": "Giải thích đáp án đúng một cách chi tiết",
                    "difficulty_level": "MEDIUM",
                    "points": 1.0,
                    "options": [
                      {"text": "Đáp án A", "is_correct": false, "feedback": "Tại sao đáp án này sai"},
                      {"text": "Đáp án B", "is_correct": true, "feedback": "Tại sao đáp án này đúng"},
                      {"text": "Đáp án C", "is_correct": false, "feedback": "Tại sao đáp án này sai"},
                      {"text": "Đáp án D", "is_correct": false, "feedback": "Tại sao đáp án này sai"}
                    ]
                  }
                ]
                """, 
                summaryText,
                request.getNumberOfQuestions(),
                request.getQuestionTypes(),
                request.getDifficultyLevel(),
                request.getLanguage(),
                request.getMcqOptionsCount(),
                additionalSection,
                difficultyInstruction,
                questionTypeInstruction);
    }
    
    private String getDifficultyInstruction(String difficultyLevel) {
        return switch (difficultyLevel.toUpperCase()) {
            case "EASY" -> """
                HƯỚNG DẪN ĐỘ KHÓ DỄ:
                - Tập trung vào khái niệm cơ bản, định nghĩa
                - Câu hỏi nhận biết, hiểu biết đơn giản
                - Tránh câu hỏi phức tạp hoặc cần suy luận sâu
                """;
            case "HARD" -> """
                HƯỚNG DẪN ĐỘ KHÓ KHÓ:
                - Câu hỏi phân tích, tổng hợp, đánh giá
                - Yêu cầu suy luận logic, áp dụng kiến thức vào tình huống mới
                - Kết hợp nhiều khái niệm, so sánh, phân biệt
                """;
            case "MIXED" -> """
                HƯỚNG DẪN ĐỘ KHÓ HỖN HỢP:
                - 30% câu dễ (nhận biết, hiểu biết)
                - 50% câu trung bình (áp dụng, phân tích)
                - 20% câu khó (tổng hợp, đánh giá)
                """;
            default -> """
                HƯỚNG DẪN ĐỘ KHÓ TRUNG BÌNH:
                - Câu hỏi áp dụng kiến thức vào tình huống cụ thể
                - Phân tích, so sánh các khái niệm
                - Cân bằng giữa lý thuyết và thực hành
                """;
        };
    }
    
    private String getQuestionTypeInstruction(String questionTypes) {
        if (questionTypes.contains("MCQ")) {
            return """
                HƯỚNG DẪN TRẮC NGHIỆM (MCQ):
                - Câu hỏi rõ ràng, không gây nhầm lẫn
                - Các lựa chọn có độ dài tương đương
                - Chỉ có 1 đáp án đúng duy nhất
                - Các đáp án sai phải hợp lý, không quá dễ loại trừ
                """;
        } else if (questionTypes.contains("TRUE_FALSE")) {
            return """
                HƯỚNG DẪN ĐÚNG/SAI:
                - Câu hỏi phải rõ ràng, tránh mơ hồ
                - Tránh từ ngữ tuyệt đối như "luôn luôn", "không bao giờ"
                - Tập trung vào một khái niệm cụ thể
                """;
        } else {
            return """
                HƯỚNG DẪN CHUNG:
                - Câu hỏi phải liên quan trực tiếp đến nội dung
                - Sử dụng ngôn ngữ phù hợp với trình độ học viên
                - Tránh câu hỏi mang tính chủ quan
                """;
        }
    }
    
    private List<ExamQuestion> convertToExamQuestions(Exam exam, List<Map<String, Object>> questionList,
                                                    GenerateQuestionsRequest request) {
        List<ExamQuestion> questions = new ArrayList<>();
        
        for (int i = 0; i < questionList.size(); i++) {
            Map<String, Object> questionData = questionList.get(i);
            
            ExamQuestion question = new ExamQuestion();
            question.setExam(exam);
            question.setQuestionText((String) questionData.get("question"));
            question.setOrderIndex(i + 1);
            
            // Set question type
            String type = (String) questionData.getOrDefault("type", "MCQ");
            question.setQuestionType(QuestionType.valueOf(type));
            
            // Set points
            Object pointsObj = questionData.get("points");
            BigDecimal points = pointsObj != null ? 
                BigDecimal.valueOf(((Number) pointsObj).doubleValue()) : BigDecimal.ONE;
            question.setPoints(points);
            
            // Set difficulty
            String difficulty = (String) questionData.getOrDefault("difficulty_level", request.getDifficultyLevel());
            question.setDifficultyLevel(DifficultyLevel.valueOf(difficulty.toUpperCase()));
            
            // Set explanation
            question.setExplanation((String) questionData.get("explanation"));
            
            // Set correct answer for grading
            try {
                Map<String, Object> correctAnswer = extractCorrectAnswer(questionData);
                question.setCorrectAnswer(objectMapper.writeValueAsString(correctAnswer));
            } catch (Exception e) {
                log.warn("Error serializing correct answer for question {}: {}", i, e.getMessage());
            }
            
            // Create options for MCQ and TRUE_FALSE questions
            if (questionData.containsKey("options")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> optionMaps = (List<Map<String, Object>>) questionData.get("options");
                List<ExamQuestionOption> options = new ArrayList<>();
                
                for (int j = 0; j < optionMaps.size(); j++) {
                    Map<String, Object> optionMap = optionMaps.get(j);
                    
                    ExamQuestionOption option = new ExamQuestionOption();
                    option.setQuestion(question);
                    option.setOptionText((String) optionMap.get("text"));
                    option.setOrderIndex(j + 1);
                    option.setIsCorrect((Boolean) optionMap.getOrDefault("is_correct", false));
                    
                    options.add(option);
                }
                
                question.setOptions(options);
            }
            
            questions.add(question);
        }
        
        return questions;
    }
    
    private Map<String, Object> extractCorrectAnswer(Map<String, Object> questionData) {
        Map<String, Object> correctAnswer = new java.util.HashMap<>();
        
        String type = (String) questionData.getOrDefault("type", "MCQ");
        
        if ("MCQ".equals(type)) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> options = (List<Map<String, Object>>) questionData.get("options");
            if (options != null) {
                for (int i = 0; i < options.size(); i++) {
                    Map<String, Object> option = options.get(i);
                    if (Boolean.TRUE.equals(option.get("is_correct"))) {
                        correctAnswer.put("correctOptionIndex", i);
                        correctAnswer.put("correctOptionText", option.get("text"));
                        break;
                    }
                }
            }
        } else if ("TRUE_FALSE".equals(type)) {
            // For TRUE_FALSE, assume first option is the answer
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> options = (List<Map<String, Object>>) questionData.get("options");
            if (options != null && !options.isEmpty()) {
                correctAnswer.put("answer", options.get(0).get("is_correct"));
            }
        }
        
        return correctAnswer;
    }
}