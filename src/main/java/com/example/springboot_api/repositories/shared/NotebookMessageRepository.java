package com.example.springboot_api.repositories.shared;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.NotebookMessage;

@Repository
public interface NotebookMessageRepository extends JpaRepository<NotebookMessage, UUID> {

    @Query("""
            SELECT nm FROM Notebook_Message nm
            JOIN FETCH nm.user u
            WHERE nm.notebook.id = :notebookId
            AND nm.type = 'user'
            ORDER BY nm.createdAt DESC
            """)
    List<NotebookMessage> findRecentByNotebookId(@Param("notebookId") UUID notebookId, Pageable pageable);

    long countByNotebookId(UUID notebookId);

    @Query("""
            SELECT COUNT(nm) FROM Notebook_Message nm
            WHERE nm.notebook.id = :notebookId
            AND nm.user.id = :userId
            """)
    long countByNotebookIdAndUserId(@Param("notebookId") UUID notebookId, @Param("userId") UUID userId);
}
