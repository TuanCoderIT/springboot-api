package com.example.springboot_api.repositories.shared;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.NotebookBotConversation;

@Repository
public interface NotebookBotConversationRepository extends JpaRepository<NotebookBotConversation, UUID> {

        @Query("""
                        SELECT COUNT(nbc) FROM Notebook_Bot_Conversation nbc
                        WHERE nbc.notebook.id = :notebookId
                        AND nbc.createdBy.id = :userId
                        """)
        long countByNotebookIdAndUserId(@Param("notebookId") UUID notebookId, @Param("userId") UUID userId);

        @Query("""
                        SELECT nbc FROM Notebook_Bot_Conversation nbc
                        WHERE nbc.notebook.id = :notebookId
                        AND nbc.createdBy.id = :userId
                        AND (:cursorNext IS NULL OR nbc.createdAt <
                            (SELECT c.createdAt FROM Notebook_Bot_Conversation c WHERE c.id = :cursorNext))
                        ORDER BY nbc.createdAt DESC, nbc.id DESC
                        """)
        List<NotebookBotConversation> findByNotebookIdAndUserIdWithCursor(
                        @Param("notebookId") UUID notebookId,
                        @Param("userId") UUID userId,
                        @Param("cursorNext") UUID cursorNext,
                        org.springframework.data.domain.Pageable pageable);

        /**
         * Tìm conversation mới nhất (theo updatedAt) của user trong notebook.
         * 
         * @param notebookId Notebook ID
         * @param userId     User ID
         * @param pageable   Pageable (chỉ lấy 1 kết quả đầu tiên)
         * @return List chứa conversation mới nhất (hoặc rỗng nếu không có)
         */
        @Query("""
                        SELECT nbc FROM Notebook_Bot_Conversation nbc
                        WHERE nbc.notebook.id = :notebookId
                        AND nbc.createdBy.id = :userId
                        ORDER BY nbc.updatedAt DESC NULLS LAST, nbc.createdAt DESC, nbc.id DESC
                        """)
        List<NotebookBotConversation> findLatestByNotebookIdAndUserId(
                        @Param("notebookId") UUID notebookId,
                        @Param("userId") UUID userId,
                        org.springframework.data.domain.Pageable pageable);
}
