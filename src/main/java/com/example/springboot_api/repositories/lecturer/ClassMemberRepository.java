package com.example.springboot_api.repositories.lecturer;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.ClassMember;

@Repository
public interface ClassMemberRepository extends JpaRepository<ClassMember, UUID> {

       @Query("SELECT cm FROM Class_Member cm " +
                     "JOIN cm.classField c " +
                     "JOIN c.teachingAssignment ta " +
                     "WHERE cm.studentCode = :studentCode AND ta.subject.id = :subjectId")
       List<ClassMember> findByStudentCodeAndSubjectId(@Param("studentCode") String studentCode,
                     @Param("subjectId") UUID subjectId);

       @Query("SELECT cm FROM Class_Member cm WHERE cm.classField.id = :classId AND cm.studentCode = :studentCode")
       ClassMember findByClassIdAndStudentCode(@Param("classId") UUID classId,
                     @Param("studentCode") String studentCode);

       @Query("SELECT COUNT(cm) FROM Class_Member cm WHERE cm.classField.id = :classId")
       long countByClassId(@Param("classId") UUID classId);

       @Query("SELECT cm FROM Class_Member cm " +
                     "WHERE cm.classField.id = :classId " +
                     "AND (:q IS NULL OR :q = '' OR LOWER(cm.fullName) LIKE LOWER(CONCAT('%', :q, '%')) " +
                     "OR LOWER(cm.studentCode) LIKE LOWER(CONCAT('%', :q, '%')))")
       Page<ClassMember> findByClassIdWithFilters(@Param("classId") UUID classId,
                     @Param("q") String keyword,
                     Pageable pageable);

       @Query("SELECT cm FROM Class_Member cm " +
                     "JOIN cm.classField c " +
                     "WHERE c.teachingAssignment.id = :assignmentId " +
                     "AND (:classId IS NULL OR c.id = :classId) " +
                     "AND (:q IS NULL OR :q = '' OR LOWER(cm.fullName) LIKE LOWER(CONCAT('%', :q, '%')) " +
                     "OR LOWER(cm.studentCode) LIKE LOWER(CONCAT('%', :q, '%')))")
       Page<ClassMember> findByAssignmentIdWithFilters(@Param("assignmentId") UUID assignmentId,
                     @Param("classId") UUID classId,
                     @Param("q") String keyword,
                     Pageable pageable);
}
