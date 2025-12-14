package com.example.springboot_api.mappers;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.example.springboot_api.dto.user.quiz.QuizListResponse;
import com.example.springboot_api.dto.user.quiz.QuizOptionResponse;
import com.example.springboot_api.dto.user.quiz.QuizResponse;
import com.example.springboot_api.models.NotebookAiSet;
import com.example.springboot_api.models.NotebookQuizOption;
import com.example.springboot_api.models.NotebookQuizz;
import com.example.springboot_api.utils.UrlNormalizer;

import lombok.RequiredArgsConstructor;

/**
 * Mapper chuyển đổi Quiz entities sang DTOs.
 */
@Component
@RequiredArgsConstructor
public class QuizMapper {

    private final UrlNormalizer urlNormalizer;

    /**
     * Convert NotebookQuizOption entity sang QuizOptionResponse DTO.
     */
    public QuizOptionResponse toQuizOptionResponse(NotebookQuizOption option) {
        if (option == null) {
            return null;
        }

        return QuizOptionResponse.builder()
                .id(option.getId())
                .text(option.getText())
                .isCorrect(option.getIsCorrect())
                .feedback(option.getFeedback())
                .position(option.getPosition())
                .build();
    }

    /**
     * Convert NotebookQuizz entity sang QuizResponse DTO.
     * Options được sort theo position.
     */
    public QuizResponse toQuizResponse(NotebookQuizz quiz) {
        if (quiz == null) {
            return null;
        }

        List<QuizOptionResponse> optionResponses = quiz.getNotebookQuizOptions().stream()
                .sorted(Comparator
                        .comparingInt(opt -> opt.getPosition() != null ? opt.getPosition() : Integer.MAX_VALUE))
                .map(this::toQuizOptionResponse)
                .toList();

        return QuizResponse.builder()
                .id(quiz.getId())
                .question(quiz.getQuestion())
                .explanation(quiz.getExplanation())
                .difficultyLevel(quiz.getDifficultyLevel())
                .createdAt(quiz.getCreatedAt())
                .options(optionResponses)
                .build();
    }

    /**
     * Convert list NotebookQuizz entities sang list QuizResponse DTOs.
     */
    public List<QuizResponse> toQuizResponseList(List<NotebookQuizz> quizzes) {
        if (quizzes == null) {
            return List.of();
        }

        return quizzes.stream()
                .map(this::toQuizResponse)
                .toList();
    }

    /**
     * Build QuizListResponse từ NotebookAiSet và danh sách QuizResponse.
     */
    public QuizListResponse toQuizListResponse(NotebookAiSet aiSet, List<QuizResponse> quizResponses) {
        if (aiSet == null) {
            return null;
        }

        UUID createdById = null;
        String createdByName = null;
        String createdByAvatar = null;

        if (aiSet.getCreatedBy() != null) {
            createdById = aiSet.getCreatedBy().getId();
            createdByName = aiSet.getCreatedBy().getFullName();
            createdByAvatar = urlNormalizer.normalizeToFull(aiSet.getCreatedBy().getAvatarUrl());
        }

        UUID notebookId = aiSet.getNotebook() != null ? aiSet.getNotebook().getId() : null;

        return QuizListResponse.builder()
                .aiSetId(aiSet.getId())
                .title(aiSet.getTitle())
                .description(aiSet.getDescription())
                .status(aiSet.getStatus())
                .errorMessage(aiSet.getErrorMessage())
                .createdAt(aiSet.getCreatedAt())
                .finishedAt(aiSet.getFinishedAt())
                .createdById(createdById)
                .createdByName(createdByName)
                .createdByAvatar(createdByAvatar)
                .notebookId(notebookId)
                .quizzes(quizResponses)
                .totalQuizzes(quizResponses.size())
                .build();
    }
}
