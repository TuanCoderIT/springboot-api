package com.example.springboot_api.repositories.lecturer;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.ClassMember;

/**
 * Repository quản lý ClassMember cho Lecturer.
 */
@Repository
public interface ClassMemberRepository extends JpaRepository<ClassMember, UUID> {

        /**
         * Lấy danh sách sinh viên theo lớp học phần với phân trang và tìm kiếm
         */
        @Query("""
                        SELECT cm FROM Class_Member cm
                        WHERE cm.classField.id = :classId
                        AND (:q IS NULL OR :q = ''
                            OR LOWER(cm.studentCode) LIKE LOWER(CONCAT('%', :q, '%'))
                            OR LOWER(cm.fullName) LIKE LOWER(CONCAT('%', :q, '%')))
                        """)
        Page<ClassMember> findByClassIdWithFilters(
                        @Param("classId") UUID classId,
                        @Param("q") String q,
                        Pageable pageable);

        /**
         * Lấy sinh viên theo assignment với filter theo classId (optional)
         */
        @Query("""
                        SELECT cm FROM Class_Member cm
                        JOIN cm.classField c
                        WHERE c.teachingAssignment.id = :assignmentId
                        AND (:classId IS NULL OR c.id = :classId)
                        AND (:q IS NULL OR :q = ''
                            OR LOWER(cm.studentCode) LIKE LOWER(CONCAT('%', :q, '%'))
                            OR LOWER(cm.fullName) LIKE LOWER(CONCAT('%', :q, '%')))
                        """)
        Page<ClassMember> findByAssignmentIdWithFilters(
                        @Param("assignmentId") UUID assignmentId,
                        @Param("classId") UUID classId,
                        @Param("q") String q,
                        Pageable pageable);

        /**
         * Đếm số sinh viên trong lớp
         */
        @Query("SELECT COUNT(cm) FROM Class_Member cm WHERE cm.classField.id = :classId")
        Long countByClassId(@Param("classId") UUID classId);

        /**
         * Đếm tổng sinh viên trong assignment (từ tất cả lớp)
         */
        @Query("""
                        SELECT COUNT(cm) FROM Class_Member cm
                        JOIN cm.classField c
                        WHERE c.teachingAssignment.id = :assignmentId
                        """)
        Long countByAssignmentId(@Param("assignmentId") UUID assignmentId);
}
