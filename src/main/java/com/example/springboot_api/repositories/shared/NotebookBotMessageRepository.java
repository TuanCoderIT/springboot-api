package com.example.springboot_api.repositories.shared;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.NotebookBotMessage;

@Repository
public interface NotebookBotMessageRepository extends JpaRepository<NotebookBotMessage, UUID> {

        /**
         * Lấy tin nhắn đầu tiên của conversation (theo createdAt ASC).
         */
        @Query("""
                        SELECT nbm FROM Notebook_Bot_Message nbm
                        WHERE nbm.conversation.id = :conversationId
                        ORDER BY nbm.createdAt ASC, nbm.id ASC
                        """)
        java.util.List<NotebookBotMessage> findByConversationIdOrderByCreatedAtAsc(
                        @Param("conversationId") UUID conversationId,
                        Pageable pageable);

        /**
         * Đếm tổng số tin nhắn trong conversation.
         */
        @Query("""
                        SELECT COUNT(nbm) FROM Notebook_Bot_Message nbm
                        WHERE nbm.conversation.id = :conversationId
                        """)
        long countByConversationId(@Param("conversationId") UUID conversationId);

        /**
         * Lấy 10 tin nhắn mới nhất của conversation, sắp xếp theo createdAt DESC.
         * Lấy cả user messages (theo user_id) và assistant messages (role =
         * 'assistant').
         * Loại trừ message hiện tại (để tránh lấy chính nó) nếu excludeMessageId không
         * null.
         */
        @Query("""
                        SELECT nbm FROM Notebook_Bot_Message nbm
                        WHERE nbm.conversation.id = :conversationId
                        AND (:excludeMessageId IS NULL OR nbm.id != :excludeMessageId)
                        AND (
                            (nbm.role = 'user' AND nbm.user.id = :userId)
                            OR nbm.role = 'assistant'
                        )
                        ORDER BY nbm.createdAt DESC, nbm.id DESC
                        """)
        java.util.List<NotebookBotMessage> findRecentMessagesByConversationIdAndUserId(
                        @Param("conversationId") UUID conversationId,
                        @Param("userId") UUID userId,
                        @Param("excludeMessageId") UUID excludeMessageId,
                        Pageable pageable);

        /**
         * Lấy 10 tin nhắn mới nhất của conversation với files (eager loading) để lấy
         * OCR text.
         * Lấy cả user messages (theo user_id) và assistant messages (role =
         * 'assistant').
         * Loại trừ message hiện tại (để tránh lấy chính nó) nếu excludeMessageId không
         * null.
         */
        @Query("""
                        SELECT DISTINCT nbm FROM Notebook_Bot_Message nbm
                        LEFT JOIN FETCH nbm.notebookBotMessageFiles
                        WHERE nbm.conversation.id = :conversationId
                        AND (:excludeMessageId IS NULL OR nbm.id != :excludeMessageId)
                        AND (
                            (nbm.role = 'user' AND nbm.user.id = :userId)
                            OR nbm.role = 'assistant'
                        )
                        ORDER BY nbm.createdAt DESC, nbm.id DESC
                        """)
        java.util.List<NotebookBotMessage> findRecentMessagesByConversationIdAndUserIdWithFiles(
                        @Param("conversationId") UUID conversationId,
                        @Param("userId") UUID userId,
                        @Param("excludeMessageId") UUID excludeMessageId,
                        Pageable pageable);

        /**
         * Lấy message với sources và files (eager loading).
         */
        @Query("""
                        SELECT DISTINCT nbm FROM Notebook_Bot_Message nbm
                        LEFT JOIN FETCH nbm.notebookBotMessageSources
                        LEFT JOIN FETCH nbm.notebookBotMessageFiles
                        WHERE nbm.id = :messageId
                        """)
        java.util.Optional<NotebookBotMessage> findByIdWithSourcesAndFiles(@Param("messageId") UUID messageId);

        /**
         * Lấy messages với cursor pagination.
         * Sắp xếp theo createdAt DESC (mới nhất trước).
         * 
         * @param conversationId Conversation ID
         * @param cursorNext     UUID của message cũ nhất từ lần load trước (null nếu là
         *                       lần đầu)
         * @param pageable       Pageable (limit 11 để check hasMore)
         * @return List<NotebookBotMessage>
         */
        @Query("""
                        SELECT DISTINCT nbm FROM Notebook_Bot_Message nbm
                        LEFT JOIN FETCH nbm.notebookBotMessageSources
                        LEFT JOIN FETCH nbm.notebookBotMessageFiles
                        WHERE nbm.conversation.id = :conversationId
                        AND (:cursorNext IS NULL OR nbm.createdAt <
                            (SELECT m.createdAt FROM Notebook_Bot_Message m WHERE m.id = :cursorNext)
                            OR (nbm.createdAt = (SELECT m.createdAt FROM Notebook_Bot_Message m WHERE m.id = :cursorNext)
                                AND nbm.id < :cursorNext))
                        ORDER BY nbm.createdAt DESC, nbm.id DESC
                        """)
        java.util.List<NotebookBotMessage> findByConversationIdWithCursor(
                        @Param("conversationId") UUID conversationId,
                        @Param("cursorNext") UUID cursorNext,
                        Pageable pageable);
}
