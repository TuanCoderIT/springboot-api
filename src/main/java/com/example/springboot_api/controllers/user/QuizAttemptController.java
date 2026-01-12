package com.example.springboot_api.controllers.user;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot_api.config.security.UserPrincipal;
import com.example.springboot_api.dto.user.quiz.attempt.AttemptResponse;
import com.example.springboot_api.dto.user.quiz.attempt.QuizAnalysisResponse;
import com.example.springboot_api.dto.user.quiz.attempt.SubmitAttemptRequest;
import com.example.springboot_api.services.shared.ai.QuizAnalysisService;
import com.example.springboot_api.services.user.QuizAttemptService;

import lombok.RequiredArgsConstructor;

/**
 * Controller cho Quiz Attempts & Analysis.
 * Base URL: /user/notebooks/{notebookId}/ai/quiz
 */
@RestController
@RequestMapping("/user/notebooks/{notebookId}/ai/quiz")
@RequiredArgsConstructor
public class QuizAttemptController {

    private final QuizAttemptService attemptService;
    private final QuizAnalysisService analysisService;

    /**
     * Submit quiz attempt - lưu kết quả làm quiz.
     * POST /user/notebooks/{notebookId}/ai/quiz/{aiSetId}/attempts
     */
    @PostMapping("/{aiSetId}/attempts")
    public ResponseEntity<AttemptResponse> submitAttempt(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID notebookId,
            @PathVariable UUID aiSetId,
            @RequestBody SubmitAttemptRequest request) {

        if (user == null) {
            throw new RuntimeException("User chưa đăng nhập.");
        }

        AttemptResponse response = attemptService.submitAttempt(user.getId(), aiSetId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy lịch sử làm quiz.
     * GET /user/notebooks/{notebookId}/ai/quiz/{aiSetId}/attempts
     */
    @GetMapping("/{aiSetId}/attempts")
    public ResponseEntity<List<AttemptResponse>> getAttemptHistory(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID notebookId,
            @PathVariable UUID aiSetId) {

        if (user == null) {
            throw new RuntimeException("User chưa đăng nhập.");
        }

        List<AttemptResponse> history = attemptService.getAttemptHistory(user.getId(), aiSetId);
        return ResponseEntity.ok(history);
    }

    /**
     * Lấy chi tiết một attempt.
     * GET /user/notebooks/{notebookId}/ai/quiz/attempts/{attemptId}
     */
    @GetMapping("/attempts/{attemptId}")
    public ResponseEntity<AttemptResponse> getAttemptDetail(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID notebookId,
            @PathVariable UUID attemptId) {

        if (user == null) {
            throw new RuntimeException("User chưa đăng nhập.");
        }

        AttemptResponse response = attemptService.getAttemptDetail(user.getId(), attemptId);
        return ResponseEntity.ok(response);
    }

    /**
     * AI phân tích kết quả quiz.
     * POST /user/notebooks/{notebookId}/ai/quiz/attempts/{attemptId}/analyze
     */
    @PostMapping("/attempts/{attemptId}/analyze")
    public ResponseEntity<QuizAnalysisResponse> analyzeAttempt(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID notebookId,
            @PathVariable UUID attemptId) {

        if (user == null) {
            throw new RuntimeException("User chưa đăng nhập.");
        }

        QuizAnalysisResponse analysis = analysisService.analyzeAttempt(attemptId);
        return ResponseEntity.ok(analysis);
    }

    /**
     * Lấy analysis đã lưu (nếu có).
     * GET /user/notebooks/{notebookId}/ai/quiz/attempts/{attemptId}/analysis
     */
    @GetMapping("/attempts/{attemptId}/analysis")
    public ResponseEntity<QuizAnalysisResponse> getSavedAnalysis(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID notebookId,
            @PathVariable UUID attemptId) {

        if (user == null) {
            throw new RuntimeException("User chưa đăng nhập.");
        }

        QuizAnalysisResponse analysis = analysisService.getSavedAnalysis(attemptId);
        if (analysis == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(analysis);
    }
}
