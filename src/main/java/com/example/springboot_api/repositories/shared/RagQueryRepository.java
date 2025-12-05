package com.example.springboot_api.repositories.shared;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.RagQuery;

@Repository
public interface RagQueryRepository extends JpaRepository<RagQuery, UUID> {

    @Query("""
            SELECT COUNT(r) FROM Rag_Query r
            WHERE r.notebook.id = :notebookId
            AND r.user.id = :userId
            """)
    long countByNotebookIdAndUserId(@Param("notebookId") UUID notebookId, @Param("userId") UUID userId);

    /**
     * Lấy lịch sử chat với cursor pagination
     * Lấy các message cũ hơn cursor (createdAt < cursor hoặc id < cursor nếu cùng
     * createdAt)
     * Sắp xếp DESC để lấy mới nhất trước, nhưng khi lướt lên sẽ lấy các message cũ
     * hơn
     */
    @Query("""
            SELECT r FROM Rag_Query r
            WHERE r.notebook.id = :notebookId
            AND r.user.id = :userId
            AND (:cursorId IS NULL OR
                 (r.createdAt < :cursorCreatedAt) OR
                 (r.createdAt = :cursorCreatedAt AND r.id < :cursorId))
            ORDER BY r.createdAt DESC, r.id DESC
            """)
    List<RagQuery> findChatHistoryByCursor(
            @Param("notebookId") UUID notebookId,
            @Param("userId") UUID userId,
            @Param("cursorId") UUID cursorId,
            @Param("cursorCreatedAt") OffsetDateTime cursorCreatedAt,
            org.springframework.data.domain.Pageable pageable);

    /**
     * Lấy lịch sử chat lần đầu (không có cursor)
     */
    @Query("""
            SELECT r FROM Rag_Query r
            WHERE r.notebook.id = :notebookId
            AND r.user.id = :userId
            ORDER BY r.createdAt DESC, r.id DESC
            """)
    List<RagQuery> findChatHistory(
            @Param("notebookId") UUID notebookId,
            @Param("userId") UUID userId,
            org.springframework.data.domain.Pageable pageable);
}
