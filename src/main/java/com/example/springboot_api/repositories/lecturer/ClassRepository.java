package com.example.springboot_api.repositories.lecturer;

import com.example.springboot_api.models.Class;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassRepository extends JpaRepository<Class, UUID> {
    
    @Query("SELECT c FROM Class c WHERE c.id = :classId")
    Optional<Class> findByIdWithDetails(@Param("classId") UUID classId);
    
    @Query("SELECT c FROM Class c " +
           "JOIN c.teachingAssignment ta " +
           "WHERE c.classCode = :classCode AND ta.subject.id = :subjectId")
    Optional<Class> findByClassCodeAndSubjectId(@Param("classCode") String classCode, 
                                               @Param("subjectId") UUID subjectId);
    
    @Query("SELECT c FROM Class c " +
           "WHERE c.teachingAssignment.id = :assignmentId " +
           "AND (:q IS NULL OR :q = '' OR LOWER(c.classCode) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Class> findByAssignmentIdWithFilters(@Param("assignmentId") UUID assignmentId,
                                             @Param("q") String keyword,
                                             Pageable pageable);
    
    @Query("SELECT c FROM Class c " +
           "JOIN c.teachingAssignment ta " +
           "WHERE ta.lecturer.id = :lecturerId " +
           "AND (:termId IS NULL OR ta.term.id = :termId) " +
           "AND (:assignmentId IS NULL OR ta.id = :assignmentId) " +
           "AND (:q IS NULL OR :q = '' OR " +
           "     LOWER(c.classCode) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "     LOWER(ta.subject.name) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Class> findAllByLecturerWithFilters(@Param("lecturerId") UUID lecturerId,
                                           @Param("termId") UUID termId,
                                           @Param("assignmentId") UUID assignmentId,
                                           @Param("q") String keyword,
                                           Pageable pageable);
}
