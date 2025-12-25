package com.example.springboot_api.repositories.exam;

import com.example.springboot_api.models.exam.Exam;
import com.example.springboot_api.models.exam.ExamStatus;
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
public interface ExamRepository extends JpaRepository<Exam, UUID> {
    
    // Tìm kỳ thi theo lớp học phần
    @Query("SELECT e FROM Exam e WHERE e.classEntity.id = :classId ORDER BY e.createdAt DESC")
    Page<Exam> findByClassId(@Param("classId") UUID classId, Pageable pageable);
    
    // Tìm kỳ thi theo giảng viên
    @Query("SELECT e FROM Exam e WHERE e.createdBy.id = :lecturerId ORDER BY e.createdAt DESC")
    Page<Exam> findByLecturerId(@Param("lecturerId") UUID lecturerId, Pageable pageable);
    
    // Tìm kỳ thi theo lớp và giảng viên
    @Query("SELECT e FROM Exam e WHERE e.classEntity.id = :classId AND e.createdBy.id = :lecturerId ORDER BY e.createdAt DESC")
    Page<Exam> findByClassIdAndLecturerId(@Param("classId") UUID classId, @Param("lecturerId") UUID lecturerId, Pageable pageable);
    
    // Tìm kỳ thi theo trạng thái
    List<Exam> findByStatusOrderByCreatedAtDesc(ExamStatus status);
    
    // Tìm kỳ thi đang hoạt động
    @Query("SELECT e FROM Exam e WHERE e.status = 'ACTIVE' AND e.startTime <= :now AND e.endTime > :now")
    List<Exam> findActiveExams(@Param("now") LocalDateTime now);
    
    // Tìm kỳ thi sắp diễn ra
    @Query("SELECT e FROM Exam e WHERE e.status = 'PUBLISHED' AND e.startTime > :now AND e.startTime <= :upcomingTime")
    List<Exam> findUpcomingExams(@Param("now") LocalDateTime now, @Param("upcomingTime") LocalDateTime upcomingTime);
    
    // Tìm kỳ thi đã hết hạn nhưng chưa đóng
    @Query("SELECT e FROM Exam e WHERE e.status = 'ACTIVE' AND e.endTime <= :now")
    List<Exam> findExpiredActiveExams(@Param("now") LocalDateTime now);
    
    // Kiểm tra sinh viên có thuộc lớp học phần không
    @Query("SELECT COUNT(cm) > 0 FROM Class_Member cm WHERE cm.classField.id = :classId AND cm.studentCode = :studentCode")
    boolean isStudentInClass(@Param("classId") UUID classId, @Param("studentCode") String studentCode);
    
    // Tìm kỳ thi mà sinh viên có thể tham gia
    @Query("SELECT e FROM Exam e " +
           "JOIN Class_Member cm ON cm.classField.id = e.classEntity.id " +
           "WHERE cm.studentCode = :studentCode " +
           "AND e.status = 'ACTIVE' " +
           "AND e.startTime <= :now " +
           "AND e.endTime > :now " +
           "ORDER BY e.startTime ASC")
    List<Exam> findAvailableExamsForStudent(@Param("studentCode") String studentCode, @Param("now") LocalDateTime now);
    
    // Tìm kỳ thi với thông tin chi tiết (chỉ fetch questions, không fetch options để tránh MultipleBagFetchException)
    @Query("SELECT e FROM Exam e " +
           "LEFT JOIN FETCH e.questions q " +
           "WHERE e.id = :examId " +
           "ORDER BY q.orderIndex ASC")
    Optional<Exam> findByIdWithQuestions(@Param("examId") UUID examId);
    
    // Đếm số câu hỏi của kỳ thi
    @Query("SELECT COUNT(q) FROM ExamQuestion q WHERE q.exam.id = :examId")
    Long countQuestionsByExamId(@Param("examId") UUID examId);
    
    // Tính tổng điểm của kỳ thi
    @Query("SELECT COALESCE(SUM(q.points), 0) FROM ExamQuestion q WHERE q.exam.id = :examId")
    Double sumPointsByExamId(@Param("examId") UUID examId);
}