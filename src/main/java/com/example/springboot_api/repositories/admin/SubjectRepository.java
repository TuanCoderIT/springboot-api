package com.example.springboot_api.repositories.admin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.Subject;

/**
 * Repository quản lý Subject cho Admin.
 */
@Repository
public interface SubjectRepository extends JpaRepository<Subject, UUID> {

    /**
     * Tìm Subject theo code
     */
    Optional<Subject> findByCode(String code);

    /**
     * Kiểm tra code đã tồn tại chưa
     */
    boolean existsByCode(String code);

    /**
     * Phân trang danh sách Subject với search và filter
     */
    @Query("""
            SELECT DISTINCT s FROM Subject s
            LEFT JOIN s.majorSubjects ms
            WHERE (:q IS NULL OR :q = '' OR LOWER(s.code) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%')))
            AND (:isActive IS NULL OR s.isActive = :isActive)
            AND (:majorId IS NULL OR ms.major.id = :majorId)
            """)
    Page<Subject> findAllSubjects(
            @Param("q") String q,
            @Param("isActive") Boolean isActive,
            @Param("majorId") UUID majorId,
            Pageable pageable);

    /**
     * Đếm số Major liên kết với Subject
     */
    @Query("SELECT COUNT(ms) FROM Major_Subject ms WHERE ms.subject.id = :subjectId")
    Long countMajorsBySubjectId(@Param("subjectId") UUID subjectId);

    /**
     * Đếm số TeachingAssignment của Subject
     */
    @Query("SELECT COUNT(ta) FROM Teaching_Assignment ta WHERE ta.subject.id = :subjectId")
    Long countAssignmentsBySubjectId(@Param("subjectId") UUID subjectId);

    /**
     * Lấy danh sách Major của Subject
     * Returns: [majorId, majorCode, majorName, termNo, isRequired, knowledgeBlock]
     */
    @Query("""
            SELECT m.id, m.code, m.name, ms.termNo, ms.isRequired, ms.knowledgeBlock
            FROM Major_Subject ms
            JOIN ms.major m
            WHERE ms.subject.id = :subjectId
            ORDER BY m.code
            """)
    List<Object[]> findMajorsOfSubject(@Param("subjectId") UUID subjectId);

    /**
     * Kiểm tra subject có TeachingAssignment không (dùng để validate trước khi xóa)
     */
    @Query("SELECT COUNT(ta) > 0 FROM Teaching_Assignment ta WHERE ta.subject.id = :subjectId")
    boolean hasAssignments(@Param("subjectId") UUID subjectId);

    /**
     * Kiểm tra subject có MajorSubject không
     */
    @Query("SELECT COUNT(ms) > 0 FROM Major_Subject ms WHERE ms.subject.id = :subjectId")
    boolean hasMajorSubjects(@Param("subjectId") UUID subjectId);

    /**
     * Đếm tổng số sinh viên đã/đang học môn này (thông qua Class)
     */
    @Query("""
            SELECT COUNT(DISTINCT cm.studentCode)
            FROM Class_Member cm
            JOIN cm.classField c
            WHERE c.teachingAssignment.subject.id = :subjectId
            """)
    Long countStudentsBySubjectId(@Param("subjectId") UUID subjectId);

    /**
     * Lấy danh sách các đợt giảng dạy của môn học (Nâng cao)
     * Returns: [assignmentId, termName, lecturerFullName, status, createdAt,
     * approvalStatus, note, lecturerEmail]
     */
    @Query("""
            SELECT ta.id, t.name, u.fullName, ta.status, ta.createdAt, ta.approvalStatus, ta.note, u.email
            FROM Teaching_Assignment ta
            JOIN ta.term t
            JOIN ta.lecturer u
            WHERE ta.subject.id = :subjectId
            ORDER BY ta.createdAt DESC
            """)
    List<Object[]> findAssignmentsBySubjectId(@Param("subjectId") UUID subjectId);

    /**
     * Lấy danh sách chi tiết các lớp học của một đợt giảng dạy
     */
    @Query("SELECT c FROM Class c WHERE c.teachingAssignment.id = :assignmentId ORDER BY c.classCode")
    List<com.example.springboot_api.models.Class> findClassesByAssignmentId(
            @Param("assignmentId") UUID assignmentId);

    /**
     * Đếm số lượng lớp học của một đợt giảng dạy
     */
    @Query("SELECT COUNT(c) FROM Class c WHERE c.teachingAssignment.id = :assignmentId")
    Long countClassesByAssignmentId(@Param("assignmentId") UUID assignmentId);
}
