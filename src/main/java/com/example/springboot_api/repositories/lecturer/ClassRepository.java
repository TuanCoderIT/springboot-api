package com.example.springboot_api.repositories.lecturer;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.Class;

/**
 * Repository quản lý Class cho Lecturer.
 */
@Repository
public interface ClassRepository extends JpaRepository<Class, UUID> {

    /**
     * Lấy danh sách lớp theo teaching assignment (không phân trang)
     */
    @Query("""
            SELECT c FROM Class c
            WHERE c.teachingAssignment.id = :assignmentId
            ORDER BY c.classCode ASC
            """)
    List<Class> findByAssignmentId(@Param("assignmentId") UUID assignmentId);

    /**
     * Lấy danh sách lớp theo assignment với phân trang, tìm kiếm
     */
    @Query("""
            SELECT c FROM Class c
            WHERE c.teachingAssignment.id = :assignmentId
            AND (:q IS NULL OR :q = ''
                OR LOWER(c.classCode) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(c.subjectName) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<Class> findByAssignmentIdWithFilters(
            @Param("assignmentId") UUID assignmentId,
            @Param("q") String q,
            Pageable pageable);

    /**
     * Đếm số lớp theo assignment
     */
    @Query("SELECT COUNT(c) FROM Class c WHERE c.teachingAssignment.id = :assignmentId")
    Long countByAssignmentId(@Param("assignmentId") UUID assignmentId);
}
