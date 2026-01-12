package com.example.springboot_api.services.exam;

import com.example.springboot_api.dto.exam.GenerateQuestionsRequest;
import com.example.springboot_api.models.NotebookFile;
import com.example.springboot_api.models.exam.Exam;
import com.example.springboot_api.models.exam.ExamQuestion;
import com.example.springboot_api.models.exam.ExamStatus;
import com.example.springboot_api.repositories.exam.ExamQuestionRepository;
import com.example.springboot_api.repositories.exam.ExamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncQuestionGenerationService {

    private final QuestionGenerationService questionGenerationService;
    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;

    @Async
    @Transactional
    public void generateQuestionsAsync(Exam exam, List<NotebookFile> notebookFiles, GenerateQuestionsRequest request) {
        log.info("🔥 [ASYNC] Bắt đầu tạo câu hỏi cho Exam ID: {}", exam.getId());

        try {
            // 1. Generate questions using existing synchronous service
            List<ExamQuestion> generatedQuestions = questionGenerationService.generateQuestions(
                    exam, notebookFiles, request);

            // 2. Save questions
            examQuestionRepository.saveAll(generatedQuestions);

            // 3. Update exam metrics
            updateExamTotals(exam);

            // 4. Update status back to DRAFT
            exam.setStatus(ExamStatus.DRAFT);
            examRepository.save(exam);

            log.info("✅ [ASYNC] Hoàn thành! Đã tạo {} câu hỏi cho Exam ID: {}",
                    generatedQuestions.size(), exam.getId());

        } catch (Exception e) {
            log.error("❌ [ASYNC] Lỗi tạo câu hỏi cho Exam ID: {}", exam.getId(), e);

            // Revert status to DRAFT on error so user can try again
            try {
                // Fetch fresh entity to avoid stale state
                Exam freshExam = examRepository.findById(exam.getId()).orElse(exam);
                freshExam.setStatus(ExamStatus.DRAFT);
                examRepository.save(freshExam);
            } catch (Exception ex) {
                log.error("Failed to revert exam status", ex);
            }
        }
    }

    private void updateExamTotals(Exam exam) {
        Long questionCount = examQuestionRepository.countByExamId(exam.getId());
        Double totalPoints = examQuestionRepository.sumPointsByExamId(exam.getId());

        exam.setTotalQuestions(questionCount.intValue());
        exam.setTotalPoints(BigDecimal.valueOf(totalPoints != null ? totalPoints : 0));

        examRepository.save(exam);
    }
}
