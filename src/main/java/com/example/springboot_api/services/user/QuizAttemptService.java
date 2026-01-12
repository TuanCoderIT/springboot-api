package com.example.springboot_api.services.user;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.springboot_api.common.exceptions.BadRequestException;
import com.example.springboot_api.common.exceptions.NotFoundException;
import com.example.springboot_api.dto.user.quiz.attempt.AttemptResponse;
import com.example.springboot_api.dto.user.quiz.attempt.SubmitAttemptRequest;
import com.example.springboot_api.models.NotebookAiSet;
import com.example.springboot_api.models.NotebookQuizOption;
import com.example.springboot_api.models.NotebookQuizz;
import com.example.springboot_api.models.QuizAttempt;
import com.example.springboot_api.models.QuizAttemptAnswer;
import com.example.springboot_api.models.User;
import com.example.springboot_api.repositories.admin.UserRepository;
import com.example.springboot_api.repositories.shared.NotebookAiSetRepository;
import com.example.springboot_api.repositories.shared.QuizAttemptAnswerRepository;
import com.example.springboot_api.repositories.shared.QuizAttemptRepository;
import com.example.springboot_api.repositories.shared.QuizOptionRepository;
import com.example.springboot_api.repositories.shared.QuizRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service xử lý quiz attempts - submit và lấy lịch sử.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuizAttemptService {

        private final QuizAttemptRepository attemptRepository;
        private final QuizAttemptAnswerRepository answerRepository;
        private final NotebookAiSetRepository aiSetRepository;
        private final UserRepository userRepository;
        private final QuizRepository quizRepository;
        private final QuizOptionRepository optionRepository;

        /**
         * Submit quiz attempt - lưu kết quả làm quiz.
         */
        @Transactional
        public AttemptResponse submitAttempt(UUID userId, UUID aiSetId, SubmitAttemptRequest request) {
                log.info("📝 [QUIZ_ATTEMPT] User {} submit attempt for AI Set {}", userId, aiSetId);

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new NotFoundException("User không tồn tại"));

                NotebookAiSet aiSet = aiSetRepository.findById(aiSetId)
                                .orElseThrow(() -> new NotFoundException("AI Set không tồn tại"));

                if (!"quiz".equals(aiSet.getSetType())) {
                        throw new BadRequestException("AI Set này không phải là quiz");
                }

                if (request.getAnswers() == null || request.getAnswers().isEmpty()) {
                        throw new BadRequestException("Danh sách câu trả lời không được trống");
                }

                // Parse time
                OffsetDateTime startedAt = request.getStartedAt() != null
                                ? OffsetDateTime.parse(request.getStartedAt())
                                : null;
                OffsetDateTime finishedAt = request.getFinishedAt() != null
                                ? OffsetDateTime.parse(request.getFinishedAt())
                                : OffsetDateTime.now();

                // Create attempt
                QuizAttempt attempt = QuizAttempt.builder()
                                .user(user)
                                .notebookAiSet(aiSet)
                                .totalQuestions(request.getAnswers().size())
                                .timeSpentSeconds(request.getTimeSpentSeconds())
                                .startedAt(startedAt)
                                .finishedAt(finishedAt)
                                .createdAt(OffsetDateTime.now())
                                .build();

                QuizAttempt savedAttempt = attemptRepository.save(attempt);

                // Save answers and calculate score
                int correctCount = 0;
                List<QuizAttemptAnswer> answers = new ArrayList<>();

                for (SubmitAttemptRequest.AnswerItem item : request.getAnswers()) {
                        NotebookQuizz quiz = quizRepository.findById(item.getQuizId()).orElse(null);
                        NotebookQuizOption selectedOption = item.getSelectedOptionId() != null
                                        ? optionRepository.findById(item.getSelectedOptionId()).orElse(null)
                                        : null;

                        boolean isCorrect = selectedOption != null
                                        && Boolean.TRUE.equals(selectedOption.getIsCorrect());
                        if (isCorrect) {
                                correctCount++;
                        }

                        QuizAttemptAnswer answer = QuizAttemptAnswer.builder()
                                        .attempt(savedAttempt)
                                        .quiz(quiz)
                                        .selectedOption(selectedOption)
                                        .isCorrect(isCorrect)
                                        .createdAt(OffsetDateTime.now())
                                        .build();
                        answers.add(answerRepository.save(answer));
                }

                // Update score
                int score = request.getAnswers().size() > 0
                                ? (int) Math.round((double) correctCount / request.getAnswers().size() * 100)
                                : 0;
                savedAttempt.setCorrectCount(correctCount);
                savedAttempt.setScore(score);
                attemptRepository.save(savedAttempt);

                log.info("✅ [QUIZ_ATTEMPT] Saved attempt {} - Score: {}/{} ({}%)",
                                savedAttempt.getId(), correctCount, request.getAnswers().size(), score);

                return toAttemptResponse(savedAttempt);
        }

        /**
         * Lấy lịch sử làm quiz của user.
         */
        @Transactional(readOnly = true)
        public List<AttemptResponse> getAttemptHistory(UUID userId, UUID aiSetId) {
                List<QuizAttempt> attempts = attemptRepository.findByUserAndAiSet(userId, aiSetId);
                return attempts.stream().map(this::toAttemptResponse).toList();
        }

        /**
         * Lấy chi tiết một attempt.
         */
        @Transactional(readOnly = true)
        public AttemptResponse getAttemptDetail(UUID userId, UUID attemptId) {
                QuizAttempt attempt = attemptRepository.findById(attemptId)
                                .orElseThrow(() -> new NotFoundException("Không tìm thấy attempt"));

                if (!attempt.getUser().getId().equals(userId)) {
                        throw new BadRequestException("Bạn không có quyền xem attempt này");
                }

                AttemptResponse response = toAttemptResponse(attempt);

                // Load answers detail
                List<QuizAttemptAnswer> answers = answerRepository.findByAttemptId(attemptId);
                List<AttemptResponse.AttemptAnswerDetail> details = new ArrayList<>();

                for (QuizAttemptAnswer answer : answers) {
                        NotebookQuizz quiz = answer.getQuiz();
                        NotebookQuizOption correctOption = quiz != null
                                        ? quiz.getNotebookQuizOptions().stream()
                                                        .filter(o -> Boolean.TRUE.equals(o.getIsCorrect()))
                                                        .findFirst().orElse(null)
                                        : null;

                        details.add(AttemptResponse.AttemptAnswerDetail.builder()
                                        .quizId(quiz != null ? quiz.getId() : null)
                                        .question(quiz != null ? quiz.getQuestion() : null)
                                        .selectedOptionId(answer.getSelectedOption() != null
                                                        ? answer.getSelectedOption().getId()
                                                        : null)
                                        .selectedOptionText(
                                                        answer.getSelectedOption() != null
                                                                        ? answer.getSelectedOption().getText()
                                                                        : null)
                                        .correctOptionId(correctOption != null ? correctOption.getId() : null)
                                        .correctOptionText(correctOption != null ? correctOption.getText() : null)
                                        .isCorrect(Boolean.TRUE.equals(answer.getIsCorrect()))
                                        .build());
                }

                response.setAnswers(details);
                return response;
        }

        private AttemptResponse toAttemptResponse(QuizAttempt attempt) {
                return AttemptResponse.builder()
                                .id(attempt.getId())
                                .aiSetId(attempt.getNotebookAiSet().getId())
                                .score(attempt.getScore())
                                .totalQuestions(attempt.getTotalQuestions())
                                .correctCount(attempt.getCorrectCount())
                                .timeSpentSeconds(attempt.getTimeSpentSeconds())
                                .startedAt(attempt.getStartedAt())
                                .finishedAt(attempt.getFinishedAt())
                                .createdAt(attempt.getCreatedAt())
                                .hasAnalysis(attempt.getAnalysisJson() != null)
                                .build();
        }
}
