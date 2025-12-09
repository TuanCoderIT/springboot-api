package com.example.springboot_api.repositories.shared;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.NotebookBotMessageFile;

@Repository
public interface NotebookBotMessageFileRepository extends JpaRepository<NotebookBotMessageFile, UUID> {

    /**
     * Lấy tất cả files của messages trong conversation.
     * 
     * @param conversationId Conversation ID
     * @return List các files
     */
    @Query("""
            SELECT nbmf FROM Notebook_Bot_Message_File nbmf
            WHERE nbmf.message.conversation.id = :conversationId
            """)
    List<NotebookBotMessageFile> findByConversationId(@Param("conversationId") UUID conversationId);
}

