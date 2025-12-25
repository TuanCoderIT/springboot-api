package com.example.springboot_api.dto.exam;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class GenerateQuestionsRequest {
    
    @NotEmpty(message = "At least one notebook file must be selected")
    private List<UUID> notebookFileIds;
    
    @NotNull(message = "Number of questions is required")
    @Min(value = 1, message = "Must generate at least 1 question")
    @Max(value = 100, message = "Cannot generate more than 100 questions at once")
    private Integer numberOfQuestions = 10;
    
    @NotBlank(message = "Question types are required")
    @Pattern(regexp = "^(MCQ|TRUE_FALSE|ESSAY|CODING|FILL_BLANK|MATCHING)(,(MCQ|TRUE_FALSE|ESSAY|CODING|FILL_BLANK|MATCHING))*$", 
             message = "Invalid question types. Allowed: MCQ, TRUE_FALSE, ESSAY, CODING, FILL_BLANK, MATCHING")
    private String questionTypes = "MCQ";
    
    @NotBlank(message = "Difficulty level is required")
    @Pattern(regexp = "^(EASY|MEDIUM|HARD|MIXED)$", 
             message = "Invalid difficulty level. Allowed: EASY, MEDIUM, HARD, MIXED")
    private String difficultyLevel = "MEDIUM";
    
    // Cấu hình chi tiết cho từng loại câu hỏi
    private Integer mcqOptionsCount = 4; // Số lựa chọn cho MCQ
    private Boolean includeExplanation = true;
    private Boolean generateImages = false;
    
    // Cấu hình AI
    private String aiModel = "gpt-4";
    private String language = "vi"; // vi, en
    
    // Phân bổ câu hỏi theo độ khó (nếu MIXED)
    private Integer easyPercentage = 30;
    private Integer mediumPercentage = 50;
    private Integer hardPercentage = 20;
    
    // Validation
    @AssertTrue(message = "Difficulty percentages must sum to 100")
    public boolean isDifficultyPercentageValid() {
        if (!"MIXED".equals(difficultyLevel)) return true;
        return easyPercentage + mediumPercentage + hardPercentage == 100;
    }
    
    @AssertTrue(message = "MCQ options count must be between 2 and 6")
    public boolean isMcqOptionsCountValid() {
        if (!questionTypes.contains("MCQ")) return true;
        return mcqOptionsCount >= 2 && mcqOptionsCount <= 6;
    }
}