package com.example.springboot_api.repositories.shared;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.springboot_api.models.AiTask;

@Repository
public interface AiTaskRepository extends JpaRepository<AiTask, UUID> {

        @Query("""
                        SELECT at FROM Ai_Task at
                        WHERE at.notebook.id = :notebookId
                        ORDER BY at.createdAt DESC
                        """)
        List<AiTask> findByNotebookId(@Param("notebookId") UUID notebookId);

        @Query("""
                        SELECT at FROM Ai_Task at
                        WHERE at.notebook.id = :notebookId
                        AND at.taskType = :taskType
                        ORDER BY at.createdAt DESC
                        """)
        List<AiTask> findByNotebookIdAndTaskType(@Param("notebookId") UUID notebookId,
                        @Param("taskType") String taskType);

        @Query("""
                        SELECT at FROM Ai_Task at
                        WHERE at.status = :status
                        ORDER BY at.createdAt ASC
                        """)
        List<AiTask> findByStatus(@Param("status") String status);

        @Query("""
                        SELECT at FROM Ai_Task at
                        WHERE at.notebook.id = :notebookId
                        AND at.status = :status
                        ORDER BY at.createdAt DESC
                        """)
        List<AiTask> findByNotebookIdAndStatus(@Param("notebookId") UUID notebookId, @Param("status") String status);

        @Query("""
                        SELECT at FROM Ai_Task at
                        WHERE at.user.id = :userId
                        ORDER BY at.createdAt DESC
                        """)
        List<AiTask> findByUserId(@Param("userId") UUID userId);

        /**
         * Lấy tasks đã hoàn thành theo notebook (cho tất cả users)
         */
        @Query("""
                        SELECT at FROM Ai_Task at
                        WHERE at.notebook.id = :notebookId
                        AND at.status = 'done'
                        ORDER BY at.createdAt DESC
                        """)
        List<AiTask> findCompletedByNotebookId(@Param("notebookId") UUID notebookId);

        /**
         * Lấy tất cả tasks của user trong notebook (bao gồm mọi status)
         */
        @Query("""
                        SELECT at FROM Ai_Task at
                        WHERE at.notebook.id = :notebookId
                        AND at.user.id = :userId
                        ORDER BY at.createdAt DESC
                        """)
        List<AiTask> findByNotebookIdAndUserId(@Param("notebookId") UUID notebookId, @Param("userId") UUID userId);

        /**
         * Lấy tasks đã hoàn thành của người khác trong notebook (không phải của user
         * hiện tại)
         */
        @Query("""
                        SELECT at FROM Ai_Task at
                        WHERE at.notebook.id = :notebookId
                        AND at.status = 'done'
                        AND (at.user.id != :userId OR at.user IS NULL)
                        ORDER BY at.createdAt DESC
                        """)
        List<AiTask> findCompletedByNotebookIdExcludeUser(@Param("notebookId") UUID notebookId,
                        @Param("userId") UUID userId);

        long countByNotebookIdAndStatus(UUID notebookId, String status);

        long countByNotebookIdAndTaskType(UUID notebookId, String taskType);
}