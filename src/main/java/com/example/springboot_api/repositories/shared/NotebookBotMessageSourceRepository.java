package com.example.springboot_api.repositories.shared;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.NotebookBotMessageSource;

@Repository
public interface NotebookBotMessageSourceRepository extends JpaRepository<NotebookBotMessageSource, UUID> {

    /**
     * Lấy tất cả sources của một message.
     */
    List<NotebookBotMessageSource> findByMessageId(UUID messageId);

    /**
     * Lấy tất cả sources của messages trong conversation.
     * 
     * @param conversationId Conversation ID
     * @return List các sources
     */
    @Query("""
            SELECT nbms FROM Notebook_Bot_Message_Source nbms
            WHERE nbms.message.conversation.id = :conversationId
            """)
    List<NotebookBotMessageSource> findByConversationId(@Param("conversationId") UUID conversationId);
}
