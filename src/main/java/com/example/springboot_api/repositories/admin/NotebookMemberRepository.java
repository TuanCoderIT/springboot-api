package com.example.springboot_api.repositories.admin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.springboot_api.models.NotebookMember;

public interface NotebookMemberRepository extends JpaRepository<NotebookMember, UUID> {

     List<NotebookMember> findByNotebookIdAndStatus(UUID notebookId, String status);

     Optional<NotebookMember> findByNotebookIdAndUserId(UUID notebookId, UUID userId);

     long countByNotebookIdAndStatus(UUID notebookId, String status);

     @Query("""
               SELECT nm FROM Notebook_Member nm
               JOIN FETCH nm.notebook n
               JOIN FETCH nm.user u
               WHERE nm.status = 'pending'
               AND (:notebookId IS NULL OR nm.notebook.id = :notebookId)
               ORDER BY nm.createdAt DESC
               """)
     Page<NotebookMember> findPendingRequests(
               @Param("notebookId") UUID notebookId,
               Pageable pageable);

     @Query("""
               SELECT nm FROM Notebook_Member nm
               JOIN FETCH nm.notebook n
               JOIN FETCH nm.user u
               WHERE (:notebookId IS NULL OR nm.notebook.id = :notebookId)
               AND (:status IS NULL OR :status = '' OR nm.status = :status)
               AND (:q IS NULL OR :q = '' OR
                    LOWER(n.title) LIKE LOWER(CONCAT('%', :q, '%')) OR
                    LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%')) OR
                    LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')))
               """)
     Page<NotebookMember> findMembersWithFilters(
               @Param("notebookId") UUID notebookId,
               @Param("status") String status,
               @Param("q") String keyword,
               Pageable pageable);

     @Query("""
               SELECT nm FROM Notebook_Member nm
               WHERE nm.user.id = :userId
               AND nm.status IN ('approved', 'pending')
               """)
     List<NotebookMember> findUserMemberships(@Param("userId") UUID userId);

     @Query("""
               SELECT nm FROM Notebook_Member nm
               WHERE nm.user.id = :userId
               AND nm.notebook.type = 'community'
               AND (:status IS NULL OR :status = '' OR nm.status = :status)
               AND (:q IS NULL OR :q = '' OR LOWER(nm.notebook.title) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(nm.notebook.description) LIKE LOWER(CONCAT('%', :q, '%')))
               """)
     Page<NotebookMember> findByUserIdAndStatus(
               @Param("userId") UUID userId,
               @Param("status") String status,
               @Param("q") String keyword,
               Pageable pageable);

     @Query("""
               SELECT DISTINCT nm FROM Notebook_Member nm
               JOIN FETCH nm.notebook n
               WHERE nm.user.id = :userId
               AND n.type = 'community'
               AND (:status IS NULL OR :status = '' OR nm.status = :status)
               AND (:q IS NULL OR :q = '' OR LOWER(n.title) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(n.description) LIKE LOWER(CONCAT('%', :q, '%')))
               """)
     List<NotebookMember> findByUserIdAndStatusForSorting(
               @Param("userId") UUID userId,
               @Param("status") String status,
               @Param("q") String keyword);

     @Query("""
               SELECT nm FROM Notebook_Member nm
               WHERE nm.notebook.id = :notebookId
               AND nm.status = 'approved'
               ORDER BY nm.joinedAt DESC
               """)
     List<NotebookMember> findApprovedMembers(@Param("notebookId") UUID notebookId);

     @Query("""
               SELECT nm FROM Notebook_Member nm
               JOIN nm.user u
               WHERE nm.notebook.id = :notebookId
               AND (:status IS NULL OR :status = '' OR nm.status = :status)
               AND (:q IS NULL OR :q = '' OR
                    LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%')) OR
                    LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')))
               """)
     Page<NotebookMember> findByNotebookIdWithFilters(
               @Param("notebookId") UUID notebookId,
               @Param("status") String status,
               @Param("q") String keyword,
               Pageable pageable);

     @Query("""
               SELECT nm FROM Notebook_Member nm
               WHERE nm.status = 'pending'
               AND (:notebookId IS NULL OR nm.notebook.id = :notebookId)
               """)
     List<NotebookMember> findAllPendingRequests(@Param("notebookId") UUID notebookId);
}
