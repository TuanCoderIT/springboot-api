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

import com.example.springboot_api.models.Major;

/**
 * Repository quản lý Major cho Admin.
 */
@Repository
public interface MajorRepository extends JpaRepository<Major, UUID> {

    /**
     * Tìm Major theo code
     */
    Optional<Major> findByCode(String code);

    /**
     * Kiểm tra code đã tồn tại chưa
     */
    boolean existsByCode(String code);

    /**
     * Phân trang danh sách Major với search và filter
     */
    @Query("""
            SELECT m FROM Major m
            LEFT JOIN FETCH m.orgUnit
            WHERE (:q IS NULL OR :q = '' OR LOWER(m.code) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(m.name) LIKE LOWER(CONCAT('%', :q, '%')))
            AND (:isActive IS NULL OR m.isActive = :isActive)
            AND (:orgUnitId IS NULL OR m.orgUnit.id = :orgUnitId)
            """)
    Page<Major> findAllMajors(
            @Param("q") String q,
            @Param("isActive") Boolean isActive,
            @Param("orgUnitId") UUID orgUnitId,
            Pageable pageable);

    /**
     * Đếm số Subject trong chương trình đào tạo của Major
     */
    @Query("SELECT COUNT(ms) FROM Major_Subject ms WHERE ms.major.id = :majorId")
    Long countSubjectsByMajorId(@Param("majorId") UUID majorId);

    /**
     * Đếm số sinh viên đang học ngành này (dựa vào User có role STUDENT và
     * major_id)
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.major.id = :majorId AND u.role = 'STUDENT'")
    Long countStudentsByMajorId(@Param("majorId") UUID majorId);

    /**
     * Lấy danh sách Subject của Major
     * Returns: [subjectId, subjectCode, subjectName, credit, termNo, isRequired,
     * knowledgeBlock]
     */
    @Query("""
            SELECT s.id, s.code, s.name, s.credit, ms.termNo, ms.isRequired, ms.knowledgeBlock
            FROM Major_Subject ms
            JOIN ms.subject s
            WHERE ms.major.id = :majorId
            ORDER BY ms.termNo, s.code
            """)
    List<Object[]> findSubjectsOfMajor(@Param("majorId") UUID majorId);

    /**
     * Kiểm tra major có sinh viên không (dùng để validate trước khi xóa)
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.major.id = :majorId AND u.role = 'STUDENT'")
    boolean hasStudents(@Param("majorId") UUID majorId);

    /**
     * Kiểm tra major có MajorSubject không
     */
    @Query("SELECT COUNT(ms) > 0 FROM Major_Subject ms WHERE ms.major.id = :majorId")
    boolean hasMajorSubjects(@Param("majorId") UUID majorId);
}
