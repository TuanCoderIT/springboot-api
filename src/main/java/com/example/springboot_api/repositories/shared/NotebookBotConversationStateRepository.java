package com.example.springboot_api.repositories.shared;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.NotebookBotConversationState;

@Repository
public interface NotebookBotConversationStateRepository extends JpaRepository<NotebookBotConversationState, UUID> {

    /**
     * Tìm conversation state theo user_id và notebook_id
     * Unique constraint đảm bảo chỉ có 1 record cho mỗi cặp (user_id, notebook_id)
     */
    @Query("""
            SELECT nbcs FROM Notebook_Bot_Conversation_State nbcs
            WHERE nbcs.user.id = :userId
            AND nbcs.notebook.id = :notebookId
            """)
    Optional<NotebookBotConversationState> findByUserIdAndNotebookId(
            @Param("userId") UUID userId,
            @Param("notebookId") UUID notebookId);
}

