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
        Pageable pageable
    );

    @Query("""
        SELECT nm FROM Notebook_Member nm
        WHERE nm.user.id = :userId
        AND nm.status IN ('approved', 'pending')
        """)
    List<NotebookMember> findUserMemberships(@Param("userId") UUID userId);

    @Query("""
        SELECT nm FROM Notebook_Member nm
        WHERE nm.notebook.id = :notebookId
        AND nm.status = 'approved'
        ORDER BY nm.joinedAt DESC
        """)
    List<NotebookMember> findApprovedMembers(@Param("notebookId") UUID notebookId);
}
