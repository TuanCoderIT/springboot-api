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

import com.example.springboot_api.models.Term;

/**
 * Repository quản lý Term cho Admin.
 */
@Repository
public interface TermRepository extends JpaRepository<Term, UUID> {

    /**
     * Tìm Term theo code
     */
    Optional<Term> findByCode(String code);

    /**
     * Kiểm tra code đã tồn tại chưa
     */
    boolean existsByCode(String code);

    /**
     * Phân trang danh sách Term với search và filter
     */
    @Query("""
            SELECT t FROM Term t
            WHERE (:q IS NULL OR :q = '' OR LOWER(t.code) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(t.name) LIKE LOWER(CONCAT('%', :q, '%')))
            AND (:isActive IS NULL OR t.isActive = :isActive)
            """)
    Page<Term> findAllTerms(
            @Param("q") String q,
            @Param("isActive") Boolean isActive,
            Pageable pageable);

    /**
     * Đếm số TeachingAssignment trong term
     */
    @Query("SELECT COUNT(ta) FROM Teaching_Assignment ta WHERE ta.term.id = :termId")
    Long countAssignmentsByTermId(@Param("termId") UUID termId);

    /**
     * Lấy danh sách môn học được mở trong term với số giảng viên
     * Returns: [subjectId, subjectCode, subjectName, credit, teacherCount]
     */
    @Query("""
            SELECT s.id, s.code, s.name, s.credit, COUNT(DISTINCT ta.lecturer.id) as teacherCount
            FROM Teaching_Assignment ta
            JOIN ta.subject s
            WHERE ta.term.id = :termId
            GROUP BY s.id, s.code, s.name, s.credit
            ORDER BY s.code
            """)
    List<Object[]> findSubjectsInTerm(@Param("termId") UUID termId);

    /**
     * Kiểm tra term có TeachingAssignment không (dùng để validate trước khi xóa)
     */
    @Query("SELECT COUNT(ta) > 0 FROM Teaching_Assignment ta WHERE ta.term.id = :termId")
    boolean hasAssignments(@Param("termId") UUID termId);

    /**
     * Lấy danh sách học kỳ khả dụng (endDate >= ngày hiện tại)
     * Dùng cho giảng viên khi xin đăng ký dạy môn
     */
    @Query("""
            SELECT t FROM Term t
            WHERE t.endDate >= CURRENT_DATE
            AND (:q IS NULL OR :q = '' OR LOWER(t.code) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(t.name) LIKE LOWER(CONCAT('%', :q, '%')))
            AND (:isActive IS NULL OR t.isActive = :isActive)
            ORDER BY t.startDate DESC
            """)
    Page<Term> findAvailableTerms(
            @Param("q") String q,
            @Param("isActive") Boolean isActive,
            Pageable pageable);
}
