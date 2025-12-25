package com.example.springboot_api.repositories.exam;

import com.example.springboot_api.models.exam.ExamQuestion;
import com.example.springboot_api.models.exam.QuestionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, UUID> {
    
    // Tìm câu hỏi theo kỳ thi
    @Query("SELECT q FROM ExamQuestion q WHERE q.exam.id = :examId ORDER BY q.orderIndex ASC")
    List<ExamQuestion> findByExamIdOrderByOrderIndex(@Param("examId") UUID examId);
    
    // Tìm câu hỏi theo kỳ thi với options
    @Query("SELECT q FROM ExamQuestion q " +
           "LEFT JOIN FETCH q.options o " +
           "WHERE q.exam.id = :examId " +
           "ORDER BY q.orderIndex ASC, o.orderIndex ASC")
    List<ExamQuestion> findByExamIdWithOptions(@Param("examId") UUID examId);
    
    // Tìm câu hỏi theo loại
    List<ExamQuestion> findByExamIdAndQuestionTypeOrderByOrderIndex(UUID examId, QuestionType questionType);
    
    // Đếm số câu hỏi theo kỳ thi
    Long countByExamId(UUID examId);
    
    // Đếm số câu hỏi theo loại
    Long countByExamIdAndQuestionType(UUID examId, QuestionType questionType);
    
    // Tìm order index lớn nhất
    @Query("SELECT COALESCE(MAX(q.orderIndex), 0) FROM ExamQuestion q WHERE q.exam.id = :examId")
    Integer findMaxOrderIndexByExamId(@Param("examId") UUID examId);
    
    // Tính tổng điểm
    @Query("SELECT COALESCE(SUM(q.points), 0) FROM ExamQuestion q WHERE q.exam.id = :examId")
    Double sumPointsByExamId(@Param("examId") UUID examId);
    
    // Xóa tất cả câu hỏi của kỳ thi
    void deleteByExamId(UUID examId);
}