package com.example.springboot_api.repositories.admin;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.TeachingAssignment;

@Repository
public interface TeachingAssignmentRepository extends JpaRepository<TeachingAssignment, UUID> {

       @Query("SELECT ta FROM Teaching_Assignment ta " +
                     "JOIN FETCH ta.term " +
                     "JOIN FETCH ta.subject " +
                     "JOIN FETCH ta.lecturer u " +
                     "WHERE ta.lecturer.id = :teacherId " +
                     "ORDER BY ta.createdAt DESC")
       List<TeachingAssignment> findByTeacherUserId(@Param("teacherId") UUID teacherId);

       @Query("SELECT ta FROM Teaching_Assignment ta " +
                     "JOIN FETCH ta.term " +
                     "JOIN FETCH ta.subject " +
                     "JOIN FETCH ta.lecturer u " +
                     "WHERE (:termId IS NULL OR ta.term.id = :termId) " +
                     "AND (:teacherId IS NULL OR ta.lecturer.id = :teacherId) " +
                     "AND (:status IS NULL OR ta.approvalStatus = :status) " +
                     "ORDER BY ta.createdAt DESC")
       List<TeachingAssignment> findAllWithFilters(
                     @Param("termId") UUID termId,
                     @Param("teacherId") UUID teacherId,
                     @Param("status") String status);

       @Query(value = "SELECT ta FROM Teaching_Assignment ta " +
                     "JOIN FETCH ta.term " +
                     "JOIN FETCH ta.subject " +
                     "JOIN FETCH ta.lecturer u " +
                     "WHERE u.id = :lecturerId " +
                     "AND (:termId IS NULL OR ta.term.id = :termId) " +
                     "AND (:status IS NULL OR ta.approvalStatus = :status)", countQuery = "SELECT COUNT(ta) FROM Teaching_Assignment ta "
                                   +
                                   "WHERE ta.lecturer.id = :lecturerId " +
                                   "AND (:termId IS NULL OR ta.term.id = :termId) " +
                                   "AND (:status IS NULL OR ta.approvalStatus = :status)")
       org.springframework.data.domain.Page<TeachingAssignment> findAllByLecturerWithFilters(
                     @Param("lecturerId") UUID lecturerId,
                     @Param("termId") UUID termId,
                     @Param("status") String status,
                     org.springframework.data.domain.Pageable pageable);

       /**
        * Đếm số lớp học trong một phân công
        */
       @Query("SELECT COUNT(c) FROM Class c WHERE c.teachingAssignment.id = :assignmentId")
       Long countClassesByAssignmentId(@Param("assignmentId") UUID assignmentId);

       /**
        * Đếm số sinh viên đăng ký các lớp của một phân công
        */
       @Query("SELECT COUNT(DISTINCT cm.studentCode) FROM Class_Member cm " +
                     "JOIN cm.classField c " +
                     "WHERE c.teachingAssignment.id = :assignmentId")
       Long countStudentsByAssignmentId(@Param("assignmentId") UUID assignmentId);

       /**
        * Kiểm tra xem đã tồn tại assignment cho lecturer + term + subject chưa
        */
       boolean existsByTermIdAndSubjectIdAndLecturerId(UUID termId, UUID subjectId, UUID lecturerId);

       @Query("SELECT ta FROM Teaching_Assignment ta WHERE ta.notebook.id = :notebookId")
       java.util.Optional<TeachingAssignment> findByNotebookId(@Param("notebookId") UUID notebookId);

       /**
        * Tìm teaching assignment theo lecturer và subject
        */
       @Query("SELECT ta FROM Teaching_Assignment ta " +
                     "WHERE ta.lecturer.id = :lecturerId AND ta.subject.id = :subjectId " +
                     "ORDER BY ta.createdAt DESC")
       List<TeachingAssignment> findByLecturerIdAndSubjectId(@Param("lecturerId") UUID lecturerId,
                     @Param("subjectId") UUID subjectId);

       /**
        * Tìm teaching assignment theo ID và fetch kèm Notebook, Subject, Term
        * Dùng cho createManualClass để kiểm tra và tạo notebook
        */
       @Query("SELECT ta FROM Teaching_Assignment ta " +
                     "LEFT JOIN FETCH ta.notebook " +
                     "JOIN FETCH ta.subject " +
                     "JOIN FETCH ta.term " +
                     "JOIN FETCH ta.lecturer " +
                     "WHERE ta.id = :id")
       java.util.Optional<TeachingAssignment> findByIdWithNotebook(@Param("id") UUID id);
}
