package com.example.springboot_api.repositories.exam;

import com.example.springboot_api.models.exam.AttemptStatus;
import com.example.springboot_api.models.exam.ExamAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, UUID> {
    
    // Tìm lượt thi theo kỳ thi và sinh viên
    @Query("SELECT ea FROM ExamAttempt ea WHERE ea.exam.id = :examId AND ea.student.id = :studentId ORDER BY ea.attemptNumber DESC")
    List<ExamAttempt> findByExamIdAndStudentId(@Param("examId") UUID examId, @Param("studentId") UUID studentId);
    
    // Tìm lượt thi theo kỳ thi và student code
    @Query("SELECT ea FROM ExamAttempt ea " +
           "JOIN User u ON ea.student.id = u.id " +
           "WHERE ea.exam.id = :examId AND u.studentCode = :studentCode " +
           "ORDER BY ea.attemptNumber DESC")
    List<ExamAttempt> findByExamIdAndStudentCode(@Param("examId") UUID examId, @Param("studentCode") String studentCode);
    
    // Tìm lượt thi hiện tại của sinh viên
    @Query("SELECT ea FROM ExamAttempt ea WHERE ea.exam.id = :examId AND ea.student.id = :studentId AND ea.status = 'IN_PROGRESS'")
    Optional<ExamAttempt> findCurrentAttempt(@Param("examId") UUID examId, @Param("studentId") UUID studentId);
    
    // Tìm lượt thi theo student code
    @Query("SELECT ea FROM ExamAttempt ea " +
           "JOIN User u ON ea.student.id = u.id " +
           "WHERE ea.exam.id = :examId AND u.studentCode = :studentCode AND ea.status = 'IN_PROGRESS'")
    Optional<ExamAttempt> findCurrentAttemptByStudentCode(@Param("examId") UUID examId, @Param("studentCode") String studentCode);
    
    // Đếm số lượt thi của sinh viên
    Long countByExamIdAndStudentId(UUID examId, UUID studentId);
    
    // Đếm số lượt thi theo student code
    @Query("SELECT COUNT(ea) FROM ExamAttempt ea " +
           "JOIN User u ON ea.student.id = u.id " +
           "WHERE ea.exam.id = :examId AND u.studentCode = :studentCode")
    Long countByExamIdAndStudentCode(@Param("examId") UUID examId, @Param("studentCode") String studentCode);
    
    // Tìm tất cả lượt thi của kỳ thi
    @Query("SELECT ea FROM ExamAttempt ea WHERE ea.exam.id = :examId ORDER BY ea.createdAt DESC")
    Page<ExamAttempt> findByExamId(@Param("examId") UUID examId, Pageable pageable);
    
    // Tìm lượt thi theo trạng thái
    List<ExamAttempt> findByExamIdAndStatusOrderByCreatedAtDesc(UUID examId, AttemptStatus status);
    
    // Tìm lượt thi đang diễn ra nhưng đã quá giờ
    @Query("SELECT ea FROM ExamAttempt ea " +
           "WHERE ea.status = 'IN_PROGRESS' " +
           "AND ea.startedAt < :timeLimit")
    List<ExamAttempt> findOverdueAttempts(@Param("timeLimit") LocalDateTime timeLimit);
    
    // Tìm lượt thi với answers
    @Query("SELECT ea FROM ExamAttempt ea " +
           "LEFT JOIN FETCH ea.answers " +
           "WHERE ea.id = :attemptId")
    Optional<ExamAttempt> findByIdWithAnswers(@Param("attemptId") UUID attemptId);
    
    // Thống kê lượt thi theo kỳ thi
    @Query("SELECT ea.status, COUNT(ea) FROM ExamAttempt ea WHERE ea.exam.id = :examId GROUP BY ea.status")
    List<Object[]> getAttemptStatsByExamId(@Param("examId") UUID examId);
    
    // Tìm điểm cao nhất của sinh viên
    @Query("SELECT MAX(ea.totalScore) FROM ExamAttempt ea WHERE ea.exam.id = :examId AND ea.student.id = :studentId AND ea.status = 'GRADED'")
    Optional<Double> findHighestScoreByExamIdAndStudentId(@Param("examId") UUID examId, @Param("studentId") UUID studentId);
    
    // Tìm lượt thi tốt nhất của sinh viên
    @Query("SELECT ea FROM ExamAttempt ea " +
           "WHERE ea.exam.id = :examId AND ea.student.id = :studentId AND ea.status = 'GRADED' " +
           "ORDER BY ea.totalScore DESC, ea.submittedAt ASC")
    List<ExamAttempt> findBestAttemptByExamIdAndStudentId(@Param("examId") UUID examId, @Param("studentId") UUID studentId, Pageable pageable);
    
    // Tìm tất cả lượt thi của kỳ thi (cho export)
    @Query("SELECT ea FROM ExamAttempt ea " +
           "LEFT JOIN FETCH ea.student " +
           "WHERE ea.exam.id = :examId " +
           "ORDER BY ea.submittedAt DESC")
    List<ExamAttempt> findByExamIdOrderBySubmittedAtDesc(@Param("examId") UUID examId);
    
    // Tìm lượt thi theo kỳ thi và lớp (cho export với class filter)
    @Query("SELECT ea FROM ExamAttempt ea " +
           "LEFT JOIN FETCH ea.student s " +
           "JOIN Class_Member cm ON cm.studentCode = s.studentCode " +
           "WHERE ea.exam.id = :examId AND cm.classField.id = :classId " +
           "ORDER BY ea.submittedAt DESC")
    List<ExamAttempt> findByExamIdAndClassId(@Param("examId") UUID examId, @Param("classId") UUID classId);
}