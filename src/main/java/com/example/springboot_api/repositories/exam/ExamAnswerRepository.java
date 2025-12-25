package com.example.springboot_api.repositories.exam;

import com.example.springboot_api.models.exam.AnswerType;
import com.example.springboot_api.models.exam.ExamAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExamAnswerRepository extends JpaRepository<ExamAnswer, UUID> {
    
    // Tìm câu trả lời theo lượt thi
    @Query("SELECT ea FROM ExamAnswer ea " +
           "JOIN FETCH ea.question q " +
           "WHERE ea.attempt.id = :attemptId " +
           "ORDER BY q.orderIndex ASC")
    List<ExamAnswer> findByAttemptIdWithQuestion(@Param("attemptId") UUID attemptId);
    
    // Tìm câu trả lời theo lượt thi và câu hỏi
    Optional<ExamAnswer> findByAttemptIdAndQuestionId(UUID attemptId, UUID questionId);
    
    // Tìm câu trả lời theo loại
    List<ExamAnswer> findByAttemptIdAndAnswerTypeOrderByQuestionOrderIndex(UUID attemptId, AnswerType answerType);
    
    // Đếm số câu trả lời
    Long countByAttemptId(UUID attemptId);
    
    // Đếm số câu trả lời đúng
    @Query("SELECT COUNT(ea) FROM ExamAnswer ea WHERE ea.attempt.id = :attemptId AND ea.isCorrect = true")
    Long countCorrectAnswersByAttemptId(@Param("attemptId") UUID attemptId);
    
    // Tính tổng điểm
    @Query("SELECT COALESCE(SUM(ea.pointsEarned), 0) FROM ExamAnswer ea WHERE ea.attempt.id = :attemptId")
    Double sumPointsEarnedByAttemptId(@Param("attemptId") UUID attemptId);
    
    // Tìm câu trả lời chưa chấm điểm
    @Query("SELECT ea FROM ExamAnswer ea WHERE ea.attempt.id = :attemptId AND ea.autoGraded = false")
    List<ExamAnswer> findUnGradedAnswersByAttemptId(@Param("attemptId") UUID attemptId);
    
    // Tìm câu trả lời tự luận cần chấm thủ công
    @Query("SELECT ea FROM ExamAnswer ea " +
           "WHERE ea.answerType IN ('ESSAY', 'CODING') " +
           "AND ea.autoGraded = false " +
           "AND ea.gradedBy IS NULL " +
           "ORDER BY ea.createdAt ASC")
    List<ExamAnswer> findEssayAnswersNeedingGrading();
    
    // Thống kê câu trả lời theo câu hỏi
    @Query("SELECT ea.question.id, ea.isCorrect, COUNT(ea) " +
           "FROM ExamAnswer ea " +
           "WHERE ea.attempt.exam.id = :examId " +
           "GROUP BY ea.question.id, ea.isCorrect")
    List<Object[]> getAnswerStatsByExamId(@Param("examId") UUID examId);
    
    // Tìm câu trả lời phổ biến nhất cho câu hỏi trắc nghiệm
    @Query("SELECT ea.answerData, COUNT(ea) as count " +
           "FROM ExamAnswer ea " +
           "WHERE ea.question.id = :questionId AND ea.answerType = 'MCQ' " +
           "GROUP BY ea.answerData " +
           "ORDER BY count DESC")
    List<Object[]> getMostCommonAnswersForQuestion(@Param("questionId") UUID questionId);
    
    // Xóa tất cả câu trả lời của lượt thi
    void deleteByAttemptId(UUID attemptId);
}