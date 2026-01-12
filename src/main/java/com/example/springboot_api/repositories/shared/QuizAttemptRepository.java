package com.example.springboot_api.repositories.shared;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.QuizAttempt;

/**
 * Repository cho QuizAttempt entity.
 */
@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {

    /**
     * Lấy lịch sử làm quiz của user theo AI Set, sắp xếp mới nhất trước.
     */
    @Query("SELECT a FROM QuizAttempt a WHERE a.user.id = :userId AND a.notebookAiSet.id = :aiSetId ORDER BY a.createdAt DESC")
    List<QuizAttempt> findByUserAndAiSet(@Param("userId") UUID userId, @Param("aiSetId") UUID aiSetId);

    /**
     * Đếm số lần user làm quiz này.
     */
    @Query("SELECT COUNT(a) FROM QuizAttempt a WHERE a.user.id = :userId AND a.notebookAiSet.id = :aiSetId")
    long countAttempts(@Param("userId") UUID userId, @Param("aiSetId") UUID aiSetId);

    /**
     * Lấy attempt gần nhất của user.
     */
    @Query("SELECT a FROM QuizAttempt a WHERE a.user.id = :userId AND a.notebookAiSet.id = :aiSetId ORDER BY a.createdAt DESC LIMIT 1")
    QuizAttempt findLatestAttempt(@Param("userId") UUID userId, @Param("aiSetId") UUID aiSetId);

    /**
     * Lấy N attempts gần nhất của user trong notebook (cross-quiz).
     * Dùng cho phân tích tiến bộ xuyên các bộ quiz.
     */
    @Query("""
            SELECT a FROM QuizAttempt a
            WHERE a.user.id = :userId
            AND a.notebookAiSet.notebook.id = :notebookId
            ORDER BY a.createdAt DESC
            """)
    List<QuizAttempt> findRecentByNotebook(@Param("userId") UUID userId,
            @Param("notebookId") UUID notebookId,
            org.springframework.data.domain.Pageable pageable);
}
